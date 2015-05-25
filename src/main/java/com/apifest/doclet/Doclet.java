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
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.apifest.api.Mapping;
import com.apifest.api.Mapping.Backend;
import com.apifest.api.Mapping.EndpointsWrapper;
import com.apifest.api.MappingAction;
import com.apifest.api.MappingDocumentation;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;
import com.apifest.api.ResponseFilter;
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

    private static final String APIFEST_INTERNAL = "apifest.internal";
    private static final String APIFEST_EXTERNAL = "apifest.external";
    private static final String APIFEST_ACTION = "apifest.action";
    private static final String APIFEST_FILTER = "apifest.filter";
    private static final String APIFEST_SCOPE = "apifest.scope";
    private static final String APIFEST_HIDDEN = "apifest.hidden";
    private static final String APIFEST_RE = "apifest.re.";
    private static final String APIFEST_BACKEND_HOST = "apifest.backend.host";
    private static final String APIFEST_BACKEND_PORT = "apifest.backend.port";
    private static final Pattern VAR_PATTERN = Pattern.compile("(\\{)(\\w*-?_?\\w*)(\\})");
    private static final String APIFEST_DOCS_DESCRIPTION = "apifest.docs.description";
    private static final String APIFEST_DOCS_SUMMARY = "apifest.docs.summary";
    private static final String APIFEST_DOCS_GROUP = "apifest.docs.group";
    private static final String APIFEST_DOCS_HIDDEN = "apifest.docs.hidden";

    // returned when a variable is missing in the properties file and then
    // passed to the Doclet as env variable
    private static final String NULL = "null";

    // valid values: user or client-app
    private static final String APIFEST_AUTH_TYPE = "apifest.auth.type";

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

    private static final String NOT_SUPPORTED_VALUE = "value \"%s\" not supported for %s tag";

    // GET, POST, PUT, DELETE, HEAD, OPTIONS
    private static List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS");

    /**
     * Starts the doclet from the command line.
     *
     * @param args
     *            List of all the packages that need to be processed.
     */
    public static void main(String[] args) {
        Doclet.cofigureDocletProperties();
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
                    ParsedEndpoint parsed = parseEndpoint(doc);
                    if (parsed != null) {
                        parsedEndpoints.add(parsed);
                    }
                }
            }
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
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
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

    private static void validateConfiguration() {
        String modeInput = System.getProperty("mode");
        if (modeInput == null) {
            throw new IllegalArgumentException("mode is invalid.");
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

    private static ParsedEndpoint parseEndpoint(MethodDoc methodDoc) throws ParseException {
        ParsedEndpoint parsed = null;
        MappingEndpoint mappingEndpoint = null;
        MappingEndpointDocumentation mappingEndpointDocumentation = null;

        String externalEndpoint = getFirstTag(methodDoc, APIFEST_EXTERNAL);
        if (externalEndpoint != null) {

            parsed = new ParsedEndpoint();
            mappingEndpoint = new MappingEndpoint();
            mappingEndpointDocumentation = new MappingEndpointDocumentation();

            mappingEndpoint.setExternalEndpoint("/" + mappingVersion + externalEndpoint);
            mappingEndpointDocumentation.setEndpoint("/" + mappingVersion + externalEndpoint);

            parseInternalEndpointTag(methodDoc, mappingEndpoint);
            parseDocsGroupTag(methodDoc, mappingEndpointDocumentation);
            parseDocsSummaryTap(methodDoc, mappingEndpointDocumentation);
            parseDocsDescriptionTag(methodDoc, mappingEndpointDocumentation);
            parseScopeTag(methodDoc, mappingEndpoint, mappingEndpointDocumentation);
            parseActionTag(methodDoc, mappingEndpoint);
            parseFilterTag(methodDoc, mappingEndpoint);
            parseAuthTypeTag(methodDoc, mappingEndpoint);
            parseEndpointBackendTags(methodDoc, mappingEndpoint);
            parseHidden(methodDoc, mappingEndpoint, mappingEndpointDocumentation);
            parseMethodAnnotations(methodDoc, mappingEndpoint, mappingEndpointDocumentation);
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

    private static void parseMethodAnnotations(MethodDoc methodDoc, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation) {
        AnnotationDesc[] annotations = methodDoc.annotations();
        for (AnnotationDesc a : annotations) {
            if (a != null && (httpMethods.contains(a.annotationType().name()))) {
                String annotationTypeName = a.annotationType().name();
                mappingEndpoint.setMethod(annotationTypeName);
                mappingEndpointDocumentation.setMethod(annotationTypeName);
            }
        }
    }

    private static void parseEndpointBackendTags(MethodDoc methodDoc, MappingEndpoint mappingEndpoint) {
        String endpointBackendHost = getFirstTag(methodDoc, APIFEST_BACKEND_HOST);
        String endpointBackendPort = getFirstTag(methodDoc, APIFEST_BACKEND_PORT);
        if (endpointBackendHost != null && endpointBackendPort != null) {
            try {
                int port = Integer.valueOf(endpointBackendPort);
                mappingEndpoint.setBackendHost(endpointBackendHost);
                mappingEndpoint.setBackendPort(port);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: apifest.backend.port " + mappingEndpoint.getExternalEndpoint() + " for endpoint is not valid, "
                        + "default backend host and port will be used");
            }
        }
    }

    private static void parseAuthTypeTag(MethodDoc methodDoc, MappingEndpoint mappingEndpoint) throws ParseException {
        String authType = getFirstTag(methodDoc, APIFEST_AUTH_TYPE);
        if (authType != null) {
            if (MappingEndpoint.AUTH_TYPE_USER.equals(authType) || MappingEndpoint.AUTH_TYPE_CLIENT_APP.equals(authType)) {
                mappingEndpoint.setAuthType(authType);
            } else {
                throw new ParseException(String.format(NOT_SUPPORTED_VALUE, authType, APIFEST_AUTH_TYPE), 0);
            }
        }
    }

    private static void parseFilterTag(MethodDoc methodDoc, MappingEndpoint mappingEndpoint) {
        String filtersTag = getFirstTag(methodDoc, APIFEST_FILTER);
        if (filtersTag != null) {
            ResponseFilter filter = new ResponseFilter();
            filter.setFilterClassName(filtersTag);
            mappingEndpoint.setFilters(filter);
        } else {
            if (defaultFilterClass != null) {
                ResponseFilter filter = new ResponseFilter();
                filter.setFilterClassName(defaultFilterClass);
                mappingEndpoint.setFilters(filter);
            }
        }
    }

    private static void parseActionTag(MethodDoc methodDoc, MappingEndpoint mappingEndpoint) {
        String actionsTag = getFirstTag(methodDoc, APIFEST_ACTION);
        if (actionsTag != null) {
            MappingAction action = new MappingAction();
            action.setActionClassName(actionsTag);
            mappingEndpoint.setAction(action);
        } else {
            if (defaultActionClass != null) {
                MappingAction action = new MappingAction();
                action.setActionClassName(defaultActionClass);
                mappingEndpoint.setAction(action);
            }
        }
    }

    private static void parseScopeTag(MethodDoc methodDoc, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String scope = getFirstTag(methodDoc, APIFEST_SCOPE);
        if (scope != null) {
            mappingEndpoint.setScope(scope);
            mappingEndpointDocumentation.setScope(scope);
        }
    }

    private static void parseDocsDescriptionTag(MethodDoc methodDoc, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String docsDecsription = getFirstTag(methodDoc, APIFEST_DOCS_DESCRIPTION);
        if (docsDecsription != null) {
            mappingEndpointDocumentation.setDescription(docsDecsription);
        }
    }

    private static void parseDocsSummaryTap(MethodDoc methodDoc, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String docsSummary = getFirstTag(methodDoc, APIFEST_DOCS_SUMMARY);
        if (docsSummary != null) {
            mappingEndpointDocumentation.setSummary(docsSummary);
        }
    }

    private static void parseDocsGroupTag(MethodDoc methodDoc, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String docsGroup = getFirstTag(methodDoc, APIFEST_DOCS_GROUP);
        if (docsGroup != null) {
            mappingEndpointDocumentation.setGroup(docsGroup);
        }
    }

    private static void parseHidden(MethodDoc methodDoc, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation) {
        boolean isHidden = getFirstTag(methodDoc, APIFEST_HIDDEN) != null;
        mappingEndpoint.setHidden(isHidden);
        isHidden = getFirstTag(methodDoc, APIFEST_DOCS_HIDDEN) != null;
        mappingEndpointDocumentation.setHidden(isHidden);
    }

    private static void parseInternalEndpointTag(MethodDoc methodDoc, MappingEndpoint mappingEndpoint) {
        String internalEndpoint = getFirstTag(methodDoc, APIFEST_INTERNAL);
        if (internalEndpoint != null) {
            if (applicationPath == null || applicationPath.isEmpty() || NULL.equals(applicationPath)) {
                mappingEndpoint.setInternalEndpoint(internalEndpoint);
            } else {
                mappingEndpoint.setInternalEndpoint(applicationPath + internalEndpoint);
            }
            Matcher m = VAR_PATTERN.matcher(internalEndpoint);
            while (m.find()) {
                String varName = m.group(2);
                // get RE if any var in internal path
                String varExpression = getFirstTag(methodDoc, APIFEST_RE + varName);
                if (varExpression != null) {
                    if (mappingEndpoint.getVarName() == null) {
                        mappingEndpoint.setVarName(varName);
                        mappingEndpoint.setVarExpression(varExpression);
                    } else {
                        // add current varName and varExpression with SPACE
                        // before that
                        mappingEndpoint.setVarName(mappingEndpoint.getVarName() + " " + varName);
                        mappingEndpoint.setVarExpression(mappingEndpoint.getVarExpression() + " " + varExpression);
                    }
                }
            }
        }
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
        mappingDocs.setMappingEndpontDocumentation(endpoints);
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

    private static String getFirstTag(MethodDoc methodDoc, String tagName) {
        Tag[] extTags = methodDoc.tags(tagName);
        if (extTags.length > 0) {
            return extTags[0].text();
        }
        return null;
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

    private static void cofigureDocletProperties() {
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
