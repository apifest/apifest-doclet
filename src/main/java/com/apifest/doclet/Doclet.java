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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.apifest.api.Mapping;
import com.apifest.api.Mapping.Backend;
import com.apifest.api.Mapping.EndpointsWrapper;
import com.apifest.api.MappingAction;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingError;
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
    private static final String APIFEST_ACTIONS = "apifest.actions";
    private static final String APIFEST_FILTERS = "apifest.filters";
    private static final String APIFEST_SCOPE = "apifest.scope";

    // valid values: user or client-app
    private static final String APIFEST_AUTH_TYPE = "apifest.auth.type";

    private static List<MappingEndpoint> endpoints = new ArrayList<MappingEndpoint>();

    // store action name as key and class as value
    private static Map<String, String> actions = new HashMap<String, String>();

    // store action name as key and class as value
    private static Map<String, String> filters = new HashMap<String, String>();

    private static String mappingVersion;

    private static String backendHost;

    private static Integer backendPort;

    private static String outputFile;

    private static final String DEFAULT_MAPPING_NAME = "output_mapping_%s.xml";

    private static final String NOT_SUPPORTED_VALUE = "value \"%s\" not supported for %s tag";

    // GET, POST, PUT, DELETE, HEAD
    private static List<String> httpMethods = Arrays.asList("javax.ws.rs.GET", "javax.ws.rs.POST",
            "javax.ws.rs.PUT", "javax.ws.rs.DELETE", "javax.ws.rs.HEAD");

    public static boolean start(RootDoc root) {
        mappingVersion = System.getProperty("mapping.version");
        if(mappingVersion == null || mappingVersion.length() == 0 || "null".equals(mappingVersion)) {
            System.out.println("ERROR: mapping.version is not set");
            return false;
        }

        backendHost = System.getProperty("backend.host");
        if(backendHost == null || backendHost.length() == 0) {
            System.out.println("ERROR: backend.host is not set");
            return false;
        }

        String backendPortStr = System.getProperty("backend.port");
        if(backendPortStr == null || backendPortStr.length() == 0) {
            System.out.println("ERROR: backend.port is not set");
            return false;
        }

        try {
            backendPort = Integer.valueOf(backendPortStr);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: backendPort is not integer");
            return false;
        }

        outputFile = System.getProperty("mapping.filename");

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
        if(outputFile == null || outputFile.length() == 0 || "null".equals(outputFile)) {
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
                endpoint.setInternalEndpoint(internalEndpoint);
            }

            String scope = getFirstTag(methodDoc, APIFEST_SCOPE);
            if (scope != null) {
                endpoint.setScope(scope);
            }

            String actionsTag = getFirstTag(methodDoc, APIFEST_ACTIONS);
            if (actionsTag != null) {
                // TODO: make it work with list of actions
                MappingAction action = new MappingAction();
                action.setActionClassName(actionsTag);
                List<MappingAction> list = new ArrayList<MappingAction>();
                list.add(action);
                endpoint.setActions(list);
            }

            String filtersTag = getFirstTag(methodDoc, APIFEST_FILTERS);
            if (filtersTag != null) {
                // TODO: make it work with list of filters
                ResponseFilter filter = new ResponseFilter();
                filter.setFilterClassName(filtersTag);
                List<ResponseFilter> list = new ArrayList<ResponseFilter>();
                list.add(filter);
                endpoint.setFilters(list);
            }

            String authType = getFirstTag(methodDoc, APIFEST_AUTH_TYPE);
            if(authType != null){
                if(MappingEndpoint.AUTH_TYPE_USER.equals(authType) || MappingEndpoint.AUTH_TYPE_CLIENT_APP.equals(authType)) {
                    endpoint.setAuthType(authType);
                } else {
                    String errorMsg = String.format(NOT_SUPPORTED_VALUE, authType, APIFEST_AUTH_TYPE);
                    throw new ParseException(errorMsg, 0);
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
