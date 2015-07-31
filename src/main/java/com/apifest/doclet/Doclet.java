/*
* Copyright 2013-2014, ApiFest project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.apifest.doclet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.apifest.api.Mapping;
import com.apifest.api.Mapping.Backend;
import com.apifest.api.Mapping.EndpointsWrapper;
import com.apifest.api.MappingDocumentation;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

/**
 * Parse Javadoc tags and create output mapping file. HTTP methods are extracted
 * from JAX-RS annotations.
 *
 * @author Rossitsa Borissova
 */
public class Doclet {

    private static final String APIFEST_EXTERNAL = "apifest.external";

    // returned when a variable is missing in the properties file and then
    // passed to the Doclet as env variable
    private static final String NULL = "null";

    // valid values: user or client-app


    private static String mappingVersion;

    private static String backendHost;

    private static Integer backendPort;

    private static String mappingOutputFile;

    private static String mappingDocsOutputFile;

    private static String applicationPath;

    // if no action is declared, use that
    private static String defaultActionClass;

    // if no filter is declared, use that
    private static String defaultFilterClass;

    private static Set<DocletMode> docletMode = new HashSet<DocletMode>();

    private static final String DEFAULT_MAPPING_NAME = "output_mapping_%s.xml";

    /**
     * Starts the doclet from the command line.
     *
     * @param args
     *            List of all the packages that need to be processed.
     */
    public static void main(String[] args) {
        Doclet.configureDocletProperties();
        String[] docletArgs = Doclet.getDocletArgs(args);
        com.sun.tools.javadoc.Main.execute(docletArgs);
    }

    public static boolean start(RootDoc root) {
        try {
            validateConfiguration();
        } catch (IllegalArgumentException ex) {
            System.out.println("ERROR: " + ex.getMessage());
            return false;
        }

        System.out.println("Start ApiFest Doclet>>>>>>>>>>>>>>>>>>>");
        System.out.println("mapping.version is: " + System.getProperty("mapping.version"));

        try {
            List<ParsedEndpoint> parsedEndpoints = new ArrayList<ParsedEndpoint>();
            ClassDoc[] classes = root.classes();
            for (ClassDoc clazz : classes) {
                MethodDoc[] mDocs = clazz.methods();
                for (MethodDoc doc : mDocs) {
                    Map<String, String> tags = extractTags(doc);
                    ParsedEndpoint parsed = parseEndpoint(tags, doc.annotations());
                    if (parsed != null) {
                        parsedEndpoints.add(parsed);
                    }
                }
            }
            EndpointComparator.orderEndpoints(parsedEndpoints);
            if (docletMode.contains(DocletMode.DOC)) {
                generateDocsFile(parsedEndpoints, mappingDocsOutputFile);
            }
            if (docletMode.contains(DocletMode.MAPPING)) {
                generateMappingFile(parsedEndpoints, mappingOutputFile);
            }
            return true;
        } catch (ParseException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        } catch (JsonGenerationException e) {
            System.out.println("ERROR: cannot create mapping documentation file, " + e.getMessage());
            return false;
        } catch (JsonMappingException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        } catch (JAXBException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        }
    }

    private static Map<String, String> extractTags(MethodDoc doc)
    {
        Map<String, String> tagMap =  new LinkedHashMap<String, String>();
        for (Tag tag: doc.tags()){
            // Strip the initial @
            String name = tag.name().startsWith("@") ? tag.name().substring(1) : tag.name();
            tagMap.put(name, tag.text());
        }
        return tagMap;
    }

    private static void validateConfiguration() {
        String modeInput = System.getProperty("mode");
        if (modeInput == null) {
            modeInput = "mapping";
        }
        String[] modesSplit = modeInput.split(",");
        for (String modeSplit : modesSplit) {
            DocletMode mode = DocletMode.fromString(modeSplit);
            if (mode == null) {
                throw new IllegalArgumentException("One of the modes you have specified is invalid: " + modeSplit);
            }
            docletMode.add(mode);
        }
        mappingVersion = System.getProperty("mapping.version");
        if (mappingVersion == null || mappingVersion.isEmpty() || NULL.equals(mappingVersion)) {
            throw new IllegalArgumentException("mapping.version is not set.");
        }
        if (docletMode.contains(DocletMode.MAPPING)) {
            backendHost = System.getProperty("backend.host");
            if (backendHost == null || backendHost.length() == 0 || NULL.equals(backendHost)) {
                throw new IllegalArgumentException("backend.host is not set.");
            }
            String backendPortStr = System.getProperty("backend.port");
            if (backendPortStr == null || backendPortStr.length() == 0 || NULL.equals(backendPort)) {
                System.out.println("ERROR: backend.port is not set");
                throw new IllegalArgumentException("backend.host is not set.");
            }
            try {
                backendPort = Integer.valueOf(backendPortStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("backendPort is not an integer.");
            }
            defaultActionClass = System.getProperty("defaultActionClass");
            defaultFilterClass = System.getProperty("defaultFilterClass");
        }
        mappingOutputFile = System.getProperty("mapping.filename");
        if (docletMode.contains(DocletMode.MAPPING) && (mappingOutputFile == null || mappingOutputFile.isEmpty())) {
            throw new IllegalArgumentException("the mappings output file must be provided.");
        }
        mappingDocsOutputFile = System.getProperty("mapping.docs.filename");
        if (docletMode.contains(DocletMode.DOC) && (mappingDocsOutputFile == null || mappingDocsOutputFile.isEmpty())) {
            throw new IllegalArgumentException("the mappings docs output file must be provided.");
        }
        applicationPath = System.getProperty("application.path");
    }

    private static ParsedEndpoint parseEndpoint(Map<String, String> tagMap, AnnotationDesc[] annotations) throws ParseException {
        ParsedEndpoint parsed = null;
        MappingEndpoint mappingEndpoint = null;
        MappingEndpointDocumentation mappingEndpointDocumentation = null;

        String externalEndpoint = tagMap.get(APIFEST_EXTERNAL);
        if (externalEndpoint != null) {

            parsed = new ParsedEndpoint();
            mappingEndpoint = new MappingEndpoint();
            mappingEndpointDocumentation = new MappingEndpointDocumentation();

            mappingEndpoint.setExternalEndpoint("/" + mappingVersion + externalEndpoint);
            mappingEndpointDocumentation.setEndpoint("/" + mappingVersion + externalEndpoint);

            Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPath);
            Parser.parseDocsDescriptiveTags(tagMap, mappingEndpointDocumentation);
            Parser.parseScopeTag(tagMap, mappingEndpoint, mappingEndpointDocumentation);
            Parser.parseActionTag(tagMap, mappingEndpoint, defaultActionClass);
            Parser.parseFilterTag(tagMap, mappingEndpoint, defaultFilterClass);
            Parser.parseAuthTypeTag(tagMap, mappingEndpoint);
            Parser.parseEndpointBackendTags(tagMap, mappingEndpoint, backendHost, backendPort);
            Parser.parseHidden(tagMap, mappingEndpoint, mappingEndpointDocumentation);
            Parser.parseMethodAnnotations(annotations, mappingEndpoint, mappingEndpointDocumentation);
            Parser.parseRequestParams(tagMap, mappingEndpointDocumentation);
            Parser.parseResultParams(tagMap, mappingEndpointDocumentation);
            Parser.parseExceptions(tagMap, mappingEndpointDocumentation);
        }

        if (parsed != null) {
            if (mappingEndpoint != null) {
                parsed.setMappingEndpoint(mappingEndpoint);
            }
            if (mappingEndpointDocumentation != null) {
                parsed.setMappingEndpointDocumentation(mappingEndpointDocumentation);
            }
        }
        return parsed;
    }



    private static void generateDocsFile(List<ParsedEndpoint> parsedEndpoints, String outputFile) throws JsonGenerationException, JsonMappingException,
            IOException {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        mapper.setAnnotationIntrospector(introspector);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        MappingDocumentation mappingDocs = new MappingDocumentation();
        List<MappingEndpointDocumentation> endpoints = new ArrayList<MappingEndpointDocumentation>();
        for (ParsedEndpoint parsed : parsedEndpoints) {
            MappingEndpointDocumentation endpoint = parsed.getMappingEndpointDocumentation();
            if (endpoint != null && !endpoint.isHidden()) {
                endpoints.add(endpoint);
            }
        }
        mappingDocs.setVersion(mappingVersion);
        mappingDocs.setMappingEndpointDocumentation(endpoints);
        mapper.writeValue(new File(outputFile), mappingDocs);
    }

    private static void generateMappingFile(List<ParsedEndpoint> parsedEndpoints, String outputFile) throws JAXBException {
        if (outputFile == null || outputFile.length() == 0 || NULL.equals(outputFile)) {
            outputFile = String.format(DEFAULT_MAPPING_NAME, mappingVersion);
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(Mapping.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Mapping mapping = new Mapping();
        mapping.setVersion(mappingVersion);
        mapping.setBackend(new Backend(backendHost, backendPort));
        EndpointsWrapper ends = new EndpointsWrapper();
        List<MappingEndpoint> endpoints = new ArrayList<MappingEndpoint>();
        for (ParsedEndpoint parsed : parsedEndpoints) {
            MappingEndpoint endpoint = parsed.getMappingEndpoint();
            if (endpoint != null && !endpoint.isHidden()) {
                endpoints.add(endpoint);
            }
        }
        ends.setEndpoints(endpoints);
        mapping.setEndpointsWrapper(ends);
        marshaller.marshal(mapping, new File(outputFile));
    }

    private static String[] getDocletArgs(String[] inputArgs) {
        String sourcePath = System.getProperty("sourcePath");
        if (sourcePath == null || sourcePath.isEmpty()) {
            throw new IllegalArgumentException("sourcePath is invalid.");
        }
        String[] argsDoclet = new String[] { "-doclet", Doclet.class.getName(), "-sourcepath", sourcePath };
        List<String> arguments = new ArrayList<String>();
        arguments.addAll(Arrays.asList(inputArgs));
        arguments.addAll(Arrays.asList(argsDoclet));
        return arguments.toArray(new String[arguments.size()]);
    }

    private static void configureDocletProperties() {
        String propertiesFilePath = System.getProperty("propertiesFilePath");
        if (propertiesFilePath == null) {
            return;
        }
        if (propertiesFilePath.isEmpty()) {
            throw new IllegalArgumentException("propertiesFilePath is invalid.");
        }
        Properties docletProps = Doclet.loadDocletProperties(propertiesFilePath);
        Enumeration<?> e = docletProps.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            System.setProperty(key, docletProps.getProperty(key));
        }
    }

    private static Properties loadDocletProperties(String filePath) {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(filePath);
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return props;
    }

}
