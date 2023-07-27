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

import java.io.*;
import java.text.ParseException;
import java.util.*;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;

import com.apifest.api.Mapping;
import com.apifest.api.Mapping.Backend;
import com.apifest.api.Mapping.EndpointsWrapper;
import com.apifest.api.MappingDocumentation;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;
import com.apifest.doclet.option.*;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;


/**
 * Parse Javadoc tags and create output mapping file. HTTP methods are extracted
 * from JAX-RS annotations.
 *
 * @author Rossitsa Borissova
 */
public class Doclet implements jdk.javadoc.doclet.Doclet {
    Reporter reporter;
    ApplicationPathOption applicationPathOption = new ApplicationPathOption();
    BackendHostOption backendHostOption = new BackendHostOption();
    BackendPortOption backendPortOption = new BackendPortOption();
    DefaultActionClassOption defaultActionClassOption = new DefaultActionClassOption();
    DefaultFilterClassOption defaultFilterClassOption = new DefaultFilterClassOption();
    MappingDocsFilenameOption mappingDocsFilenameOption = new MappingDocsFilenameOption();
    MappingFilenameOption mappingFilenameOption = new MappingFilenameOption();
    MappingVersionOption mappingVersionOption = new MappingVersionOption();
    ModeOption modeOption = new ModeOption();
    CustomAnnotationOption customAnnotationOption = new CustomAnnotationOption();
    private final Set<Option> supportedOptions = Set.of(
            applicationPathOption, backendHostOption, backendPortOption,
            defaultActionClassOption, defaultFilterClassOption, mappingDocsFilenameOption,
            mappingFilenameOption, mappingVersionOption, modeOption,
            customAnnotationOption
    );
    DocletEnvironment docEnv;
    public static List<String> errors = new ArrayList<>();

    @Override
    public void init(Locale locale, Reporter reporter) {
        // Initialize your doclet
        System.out.println("HELLO INIT METHOD");
        this.reporter = reporter;
        // You can now access the options via reporter.getOptions()
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return supportedOptions;
    }

    private static final String APIFEST_EXTERNAL = "apifest.external";

    // returned when a variable is missing in the properties file and then
    // passed to the Doclet as env variable
    private static final String NULL = "null";

    // valid values: user or client-app

    private static final String DEFAULT_MAPPING_NAME = "output_mapping_%s.xml";

    /**
     * Starts the doclet from the command line.
     *
     * @param args
     *            List of all the packages that need to be processed.
     */
    public static void main(String[] args) {
//        Doclet.configureDocletProperties();
//        String[] docletArgs = Doclet.getDocletArgs(args);
        String[] ass = new String[args.length + 1];
        ass[0] = "/usr/local/java/jdk-17.0.7/bin/javadoc";
        for (int i = 0; i < args.length; i++) {
            ass[i + 1] = args[i];
        }
        ProcessBuilder pb = new ProcessBuilder(ass);
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean run(DocletEnvironment docEnv) {
        this.docEnv = docEnv;
        if (!validateConfiguration()) {
            return false;
        }
        List<ParsedEndpoint> parsedEndpoints = new ArrayList<>();
        for (Element element : docEnv.getIncludedElements()) {
            if (element.getKind() != ElementKind.INTERFACE) {
                continue;
            }
            TypeElement classElement = (TypeElement) element;
            for (Element enclosedElement : classElement.getEnclosedElements()) {
                if (!(enclosedElement instanceof ExecutableElement methodElement)) {
                    continue;
                }
                Map<String, String> tags = extractTags(methodElement);
                ParsedEndpoint parsed;
                try {
                    parsed = parseEndpoint(tags, docEnv.getElementUtils().getAllAnnotationMirrors(element));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (parsed != null) {
                    parsedEndpoints.add(parsed);
                }
            }
        }

        EndpointComparator.orderEndpoints(parsedEndpoints);
        try {
            if (modeOption.getDocletModes().contains(DocletMode.DOC)) {
                generateDocsFile(parsedEndpoints, mappingDocsFilenameOption.getMappingDocsFilename());
            }
            if (modeOption.getDocletModes().contains(DocletMode.MAPPING)) {
                generateMappingFile(parsedEndpoints, mappingFilenameOption.getMappingFilename());
            }
        } catch (JsonGenerationException e) {
            System.out.println("ERROR: cannot create mapping documentation file, " + e.getMessage());
            return false;
        } catch (JsonMappingException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        } catch (IOException | JAXBException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        }
        return true;
    }

    class TagScanner extends SimpleDocTreeVisitor<Void, Void> {
        private final Map<String, String> tags;

        TagScanner(Map<String, String> tags) {
            this.tags = tags;
        }

        @Override
        public Void visitDocComment(DocCommentTree tree, Void p) {
            return visit(tree.getBlockTags(), null);
        }

        @Override
        public Void visitUnknownBlockTag(UnknownBlockTagTree tree, Void p) {
            String name = tree.getTagName();
            String content = tree.getContent().toString();
            if (!tags.containsKey(name)) {
                tags.put(name, content);
            }
            return null;
        }
    }

    private Map<String, String> extractTags(ExecutableElement method) {
        Map<String, String> tagMap = new TreeMap<>();
        DocCommentTree docCommentTree = docEnv.getDocTrees().getDocCommentTree(method);
        if (docCommentTree == null) {
            return tagMap;
        }
        TagScanner tagScanner = new TagScanner(tagMap);
        tagScanner.visit(docCommentTree, null);
        return tagMap;
    }

    private boolean validateConfiguration() {
        String mappingVersion = mappingVersionOption.getMappingVersion();
        if (mappingVersion == null || mappingVersion.isEmpty() || NULL.equalsIgnoreCase(mappingVersion)) {
            throw new IllegalArgumentException("mapping.version is not set.");
        }
        if (modeOption.getDocletModes().contains(DocletMode.MAPPING)) {
            String backendHost = backendHostOption.getBackendHost();
            if (backendHost == null || backendHost.isEmpty() || NULL.equalsIgnoreCase(backendHost)) {
                throw new IllegalArgumentException("backend.host is not set.");
            }
            System.out.println("customAnnotationsValue: " + customAnnotationOption.getCustomAnnotations().keySet());
        }
        String mappingOutputFile = mappingFilenameOption.getMappingFilename();
        if (modeOption.getDocletModes().contains(DocletMode.MAPPING) && (mappingOutputFile == null || mappingOutputFile.isEmpty())) {
            throw new IllegalArgumentException("the mappings output file must be provided.");
        }
        String mappingDocsOutputFile = mappingDocsFilenameOption.getMappingDocsFilename();
        if (modeOption.getDocletModes().contains(DocletMode.DOC) && (mappingDocsOutputFile == null || mappingDocsOutputFile.isEmpty())) {
            throw new IllegalArgumentException("the mappings docs output file must be provided.");
        }
        return true;
    }

    private ParsedEndpoint parseEndpoint(Map<String, String> tagMap, List<? extends AnnotationMirror> annotations) throws ParseException {
        ParsedEndpoint parsed = null;
        MappingEndpoint mappingEndpoint = null;
        MappingEndpointDocumentation mappingEndpointDocumentation = null;

        String externalEndpoint = tagMap.get(APIFEST_EXTERNAL);
        if (externalEndpoint != null) {

            parsed = new ParsedEndpoint();
            mappingEndpoint = new MappingEndpoint();
            mappingEndpointDocumentation = new MappingEndpointDocumentation();

            mappingEndpoint.setExternalEndpoint("/" + mappingVersionOption.getMappingVersion() + externalEndpoint);
            mappingEndpointDocumentation.setEndpoint("/" + mappingVersionOption.getMappingVersion() + externalEndpoint);

            Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPathOption.getApplicationPath());
            Parser.parseDocsDescriptiveTags(tagMap, mappingEndpointDocumentation);
            Parser.parseScopeTag(tagMap, mappingEndpoint, mappingEndpointDocumentation);
            Parser.parseActionTag(tagMap, mappingEndpoint, defaultActionClassOption.getDefaultActionClass());
            Parser.parseFilterTag(tagMap, mappingEndpoint, defaultFilterClassOption.getDefaultFilterClass());
            Parser.parseAuthTypeTag(tagMap, mappingEndpoint);
            Parser.parseEndpointBackendTags(tagMap, mappingEndpoint, backendHostOption.getBackendHost(), backendPortOption.getBackendPort());
            Parser.parseHidden(tagMap, mappingEndpoint, mappingEndpointDocumentation);
            Parser.parseMethodAnnotations(annotations,
                    mappingEndpoint,
                    mappingEndpointDocumentation,
                    customAnnotationOption.getCustomAnnotations());
            Parser.parseRequestParams(tagMap, mappingEndpointDocumentation);
            Parser.parseResultParams(tagMap, mappingEndpointDocumentation);
            Parser.parseExceptions(tagMap, mappingEndpointDocumentation);
        }

        if (parsed != null) {
            parsed.setMappingEndpoint(mappingEndpoint);
            parsed.setMappingEndpointDocumentation(mappingEndpointDocumentation);
        }
        return parsed;
    }

    private void generateDocsFile(List<ParsedEndpoint> parsedEndpoints, String outputFile) throws JsonGenerationException, JsonMappingException,
            IOException {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance());
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
        mappingDocs.setVersion(mappingVersionOption.getMappingVersion());
        mappingDocs.setMappingEndpointDocumentation(endpoints);
        mapper.writeValue(new File(outputFile), mappingDocs);
    }

    private void generateMappingFile(List<ParsedEndpoint> parsedEndpoints, String outputFile) throws JAXBException {
        String mappingVersion = mappingVersionOption.getMappingVersion();
        String backendHost = backendHostOption.getBackendHost();
        int backendPort = backendPortOption.getBackendPort();
        if (outputFile == null || outputFile.isEmpty() || NULL.equalsIgnoreCase(outputFile)) {
            outputFile = String.format(DEFAULT_MAPPING_NAME, mappingVersion);
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(Mapping.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        try {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
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
