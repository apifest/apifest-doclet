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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.apifest.api.Mapping;
import com.apifest.api.Mapping.Backend;
import com.apifest.api.Mapping.EndpointsWrapper;
import com.apifest.api.MappingAction;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.ResponseFilter;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

/**
 * Parse Javadoc tags and create output mapping file.
 * HTTP methods are extracted from JAX-RS annotations.
 *
 * @author Rossitsa Borissova
 */
public class Doclet {

    private static final String APIFEST_INTERNAL = "apifest.internal";
    private static final String APIFEST_EXTERNAL = "apifest.external";
    private static final String APIFEST_ACTION = "apifest.action";
    private static final String APIFEST_FILTER = "apifest.filter";
    private static final String APIFEST_SCOPE = "apifest.scope";
    private static final String APIFEST_RE = "apifest.re.";
    private static final String APIFEST_BACKEND_HOST = "apifest.backend.host";
    private static final String APIFEST_BACKEND_PORT = "apifest.backend.port";
    private static final Pattern VAR_PATTERN = Pattern.compile("(\\{)(\\w*-?_?\\w*)(\\})");

    // returned when a variable is missing in the properties file and then passed to the Doclet as env variable
    private static final String NULL = "null";

    // valid values: user or client-app
    private static final String APIFEST_AUTH_TYPE = "apifest.auth.type";

    private static List<MappingEndpoint> endpoints = new ArrayList<MappingEndpoint>();

    private static String mappingVersion;

    private static String backendHost;

    private static Integer backendPort;

    private static String outputFile;

    private static String applicationPath;

    // if no action is declared, use that
    private static String defaultActionClass;

    // if no filter is declared, use that
    private static String defaultFilterClass;

    private static final String DEFAULT_MAPPING_NAME = "output_mapping_%s.xml";

    private static final String NOT_SUPPORTED_VALUE = "value \"%s\" not supported for %s tag";

    // GET, POST, PUT, DELETE, HEAD
    private static List<String> httpMethods = Arrays.asList("javax.ws.rs.GET", "javax.ws.rs.POST",
            "javax.ws.rs.PUT", "javax.ws.rs.DELETE", "javax.ws.rs.HEAD");

    public static boolean start(RootDoc root) {
        mappingVersion = System.getProperty("mapping.version");
        if(mappingVersion == null || mappingVersion.isEmpty() || NULL.equals(mappingVersion)) {
            System.out.println("ERROR: mapping.version is not set");
            return false;
        }

        backendHost = System.getProperty("backend.host");
        if(backendHost == null || backendHost.length() == 0 || NULL.equals(backendHost)) {
            System.out.println("ERROR: backend.host is not set");
            return false;
        }

        String backendPortStr = System.getProperty("backend.port");
        if(backendPortStr == null || backendPortStr.length() == 0 || NULL.equals(backendPort)) {
            System.out.println("ERROR: backend.port is not set");
            return false;
        }

        try {
            backendPort = Integer.valueOf(backendPortStr);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: backendPort is not an integer");
            return false;
        }

        defaultActionClass = System.getProperty("defaultActionClass");

        defaultFilterClass = System.getProperty("defaultFilterClass");

        outputFile = System.getProperty("mapping.filename");

        applicationPath = System.getProperty("application.path");

        System.out.println("Start ApiFest Doclet>>>>>>>>>>>>>>>>>>>");
        System.out.println("mapping.version is: " + System.getProperty("mapping.version"));
        System.out.println("backend.host: " + System.getProperty("backend.host"));
        System.out.println("backend.port: " + System.getProperty("backend.port"));

        try {
            ClassDoc[] classes = root.classes();
            for (ClassDoc clazz : classes) {
                MethodDoc[] mDocs = clazz.methods();
                for (MethodDoc doc : mDocs) {
                    MappingEndpoint endpoint = getMappingEndpoint(doc);
                    if(endpoint != null) {
                        endpoints.add(endpoint);
                    }
                }
            }

            generateMappingFile(outputFile);
        } catch (JAXBException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        } catch (ParseException e) {
            System.out.println("ERROR: cannot create mapping file, " + e.getMessage());
            return false;
        }
        return true;
    }

    private static void generateMappingFile(String outputFile) throws JAXBException {
        if(outputFile == null || outputFile.length() == 0 || NULL.equals(outputFile)) {
            outputFile = String.format(DEFAULT_MAPPING_NAME, mappingVersion);
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(Mapping.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Mapping mapping = new Mapping();
        mapping.setVersion(mappingVersion);
        mapping.setBackend(new Backend(backendHost, backendPort));
        EndpointsWrapper ends = new EndpointsWrapper();
        ends.setEndpoints(endpoints);
        mapping.setEndpointsWrapper(ends);
        marshaller.marshal(mapping, new File(outputFile));
    }

    private static MappingEndpoint getMappingEndpoint(MethodDoc methodDoc) throws ParseException {
        MappingEndpoint endpoint = null;

        String externalEndpoint = getFirstTag(methodDoc, APIFEST_EXTERNAL);
        if(externalEndpoint != null){
            endpoint = new MappingEndpoint();
            endpoint.setExternalEndpoint("/" + mappingVersion + externalEndpoint);

            String internalEndpoint = getFirstTag(methodDoc, APIFEST_INTERNAL);
            if (internalEndpoint != null) {
                if (applicationPath == null || applicationPath.isEmpty() || NULL.equals(applicationPath)) {
                    endpoint.setInternalEndpoint(internalEndpoint);
                } else {
                    endpoint.setInternalEndpoint(applicationPath + internalEndpoint);
                }
                Matcher m = VAR_PATTERN.matcher(internalEndpoint);
                int i = 1;
                while(m.find()) {
                    String varName = m.group(2);

                    //get RE if any var in internal path
                    String varExpression = getFirstTag(methodDoc, APIFEST_RE + varName);
                    if(varExpression != null) {
                        if (endpoint.getVarName() == null) {
                            endpoint.setVarName(varName);
                            endpoint.setVarExpression(varExpression);
                        } else {
                            // add current varName and varExpression with SPACE before that
                            endpoint.setVarName(endpoint.getVarName() + " " + varName);
                            endpoint.setVarExpression(endpoint.getVarExpression() + " " + varExpression);
                        }
                    }
                }
            }

            String scope = getFirstTag(methodDoc, APIFEST_SCOPE);
            if (scope != null) {
                endpoint.setScope(scope);
            }

            String actionsTag = getFirstTag(methodDoc, APIFEST_ACTION);
            if (actionsTag != null) {
                MappingAction action = new MappingAction();
                action.setActionClassName(actionsTag);
                endpoint.setAction(action);
            } else {
                if (defaultActionClass != null) {
                    MappingAction action = new MappingAction();
                    action.setActionClassName(defaultActionClass);
                    endpoint.setAction(action);
                }
            }

            String filtersTag = getFirstTag(methodDoc, APIFEST_FILTER);
            if (filtersTag != null) {
                ResponseFilter filter = new ResponseFilter();
                filter.setFilterClassName(filtersTag);
                endpoint.setFilters(filter);
            }  else {
                if (defaultFilterClass != null) {
                    ResponseFilter filter = new ResponseFilter();
                    filter.setFilterClassName(defaultFilterClass);
                    endpoint.setFilters(filter);
                }
            }

            String authType = getFirstTag(methodDoc, APIFEST_AUTH_TYPE);
            if(authType != null) {
                if(MappingEndpoint.AUTH_TYPE_USER.equals(authType) || MappingEndpoint.AUTH_TYPE_CLIENT_APP.equals(authType)) {
                    endpoint.setAuthType(authType);
                } else {
                    String errorMsg = String.format(NOT_SUPPORTED_VALUE, authType, APIFEST_AUTH_TYPE);
                    throw new ParseException(errorMsg, 0);
                }
            }

            String endpointBackendHost = getFirstTag(methodDoc, APIFEST_BACKEND_HOST);
            String endpointBackendPort = getFirstTag(methodDoc, APIFEST_BACKEND_PORT);
            if(endpointBackendHost != null && endpointBackendPort != null) {
               try {
                   int port = Integer.valueOf(endpointBackendPort);
                   endpoint.setBackendHost(endpointBackendHost);
                   endpoint.setBackendPort(port);
               } catch (NumberFormatException e) {
                   System.out.println("ERROR: apifest.backend.port " + endpoint.getExternalEndpoint() + " for endpoint is not valid, " +  "default backend host and port will be used");
               }
            }

            AnnotationDesc[] annotations = methodDoc.annotations();
            for (AnnotationDesc a : annotations) {
                if (a != null && (httpMethods.contains(a.annotationType().toString()))) {
                    endpoint.setMethod(a.annotationType().name());
                }
            }
        }
        return endpoint;
    }

    private static String getFirstTag(MethodDoc methodDoc, String tagName) {
        Tag[] extTags = methodDoc.tags(tagName);
        if (extTags.length > 0) {
            return extTags[0].text();
        }
        return null;
    }
}
