/*
* Copyright 2013-2015, ApiFest project
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.apifest.api.MappingAction;
import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;
import com.apifest.api.ResponseFilter;
import com.apifest.api.params.ExceptionDocumentation;
import com.apifest.api.params.RequestParamDocumentation;
import com.apifest.api.params.ResultParamDocumentation;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationValue;

public class Parser
{
    private static final String APIFEST_DOCS_EXAMPLE_RESULT = "apifest.docs.exampleResult";
    private static final String APIFEST_DOCS_EXAMPLE_REQUEST = "apifest.docs.exampleRequest";
    private static final String APIFEST_DOCS_ORDER = "apifest.docs.order";
    private static final String APIFEST_RESULT_PARAMS_DESCRIPTION = "apifest.docs.resultsDescription";
    private static final String RESULT_PARAMS_PREFIX = "apifest.docs.results.";
    private static final String APIFEST_PARAMS_DESCRIPTION = "apifest.docs.paramsDescription";
    private static final String REQUEST_PARAMS_PREFIX = "apifest.docs.params.";
    private static final String APIFEST_INTERNAL = "apifest.internal";

    private static final String APIFEST_ACTION = "apifest.action";
    private static final String APIFEST_FILTER = "apifest.filter";
    private static final String APIFEST_SCOPE = "apifest.scope";
    private static final String APIFEST_HIDDEN = "apifest.hidden";
    private static final String APIFEST_RE = "apifest.re.";
    private static final String APIFEST_BACKEND_HOST = "apifest.backend.host";
    private static final String APIFEST_BACKEND_PORT = "apifest.backend.port";
    private static final Pattern VAR_PATTERN = Pattern.compile("(\\{)(\\w*-?_?\\w*)(\\})");
    private static final Pattern PARAM_PATTERN = Pattern.compile("apifest\\.docs\\.params\\.([\\w\\d_]+)");
    private static final Pattern RESULT_PARAM_PATTERN = Pattern.compile("apifest\\.docs\\.results\\.([\\w\\d_]+)");
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("apifest\\.docs\\.exceptions\\.([\\w\\d_]+)");
    private static final String APIFEST_DOCS_DESCRIPTION = "apifest.docs.description";
    private static final String APIFEST_DOCS_SUMMARY = "apifest.docs.summary";
    private static final String APIFEST_DOCS_GROUP = "apifest.docs.group";
    private static final String APIFEST_DOCS_HIDDEN = "apifest.docs.hidden";
    private static final String APIFEST_AUTH_TYPE = "apifest.auth.type";

    private static final String NOT_SUPPORTED_VALUE = "value \"%s\" not supported for %s tag";

    // GET, POST, PUT, DELETE, HEAD, OPTIONS
    private static List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS");
    private static final String NULL = "null";

    static void parseMethodAnnotations(AnnotationDesc[] annotations,
            MappingEndpoint mappingEndpoint,
            MappingEndpointDocumentation mappingEndpointDocumentation,
            Map<String, List<String>> customAnnotations) {
        for (AnnotationDesc a : annotations) {
            if (a != null && (httpMethods.contains(a.annotationType().name()))) {
                String annotationTypeName = a.annotationType().name();
                mappingEndpoint.setMethod(annotationTypeName);
                mappingEndpointDocumentation.setMethod(annotationTypeName);
                continue;
            }
            List<String> propertiesToRead = customAnnotations.get(a.annotationType().qualifiedName());
            if (propertiesToRead != null) {
                for (ElementValuePair elementValuePair : a.elementValues()) {
                    if (propertiesToRead.isEmpty() || propertiesToRead.contains(elementValuePair.element().name())) {
                        if (mappingEndpoint.getCustomProperties() == null) {
                            mappingEndpoint.setCustomProperties(new HashMap<String, String>());
                        }
                        if (mappingEndpointDocumentation.getCustomProperties() == null) {
                            mappingEndpointDocumentation.setCustomProperties(new HashMap<String, String>());
                        }
                        String valueString;
                        if (elementValuePair.value().value() instanceof AnnotationValue[]) {
                            AnnotationValue[] value = (AnnotationValue[]) elementValuePair.value().value();
                            StringBuilder builder = new StringBuilder();
                            for (AnnotationValue annotationValue : value) {
                                if (builder.length() > 0) {
                                    builder.append(",");
                                }
                                builder.append(annotationValue.value().toString());
                            }
                            valueString = builder.toString();
                        } else {
                            valueString = elementValuePair.value().toString();
                        }
                        mappingEndpoint.getCustomProperties()
                                .put(elementValuePair.element().qualifiedName(),
                                        valueString);
                        mappingEndpointDocumentation.getCustomProperties()
                                .put(elementValuePair.element().qualifiedName(),
                                        valueString);
                    }
                }
            }

        }
    }

    static void parseEndpointBackendTags(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, String defaultBackendHost, Integer defaultBackendPort) {
        String endpointBackendHost = tagMap.get(APIFEST_BACKEND_HOST);
        String endpointBackendPort = tagMap.get(APIFEST_BACKEND_PORT);
        if (endpointBackendHost != null && endpointBackendPort != null) {
            try {
                int port = Integer.valueOf(endpointBackendPort);
                mappingEndpoint.setBackendHost(endpointBackendHost);
                mappingEndpoint.setBackendPort(port);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: apifest.backend.port " + mappingEndpoint.getExternalEndpoint() + " for endpoint is not valid, "
                        + "default backend host and port will be used");
                mappingEndpoint.setBackendHost(defaultBackendHost);
                mappingEndpoint.setBackendPort(defaultBackendPort);
            }
        } else {
            mappingEndpoint.setBackendHost(defaultBackendHost);
            mappingEndpoint.setBackendPort(defaultBackendPort);
        }
    }

    static void parseAuthTypeTag(Map<String, String> tagMap, MappingEndpoint mappingEndpoint) throws ParseException {
        String authType = tagMap.get(APIFEST_AUTH_TYPE);
        if (authType != null) {
            if (MappingEndpoint.AUTH_TYPE_USER.equals(authType) || MappingEndpoint.AUTH_TYPE_CLIENT_APP.equals(authType)) {
                mappingEndpoint.setAuthType(authType);
            } else {
                throw new ParseException(String.format(NOT_SUPPORTED_VALUE, authType, APIFEST_AUTH_TYPE), 0);
            }
        }
    }

    static void parseFilterTag(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, String defaultFilterClass) {
        String filtersTag = tagMap.get(APIFEST_FILTER);
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

    static void parseActionTag(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, String defaultActionClass) {
        String actionsTag = tagMap.get(APIFEST_ACTION);
        if (actionsTag != null) {
            if(!actionsTag.equals("None")) {
                MappingAction action = new MappingAction();
                action.setActionClassName(actionsTag);
                mappingEndpoint.setAction(action);
            }
        } else {
            if (defaultActionClass != null) {
                MappingAction action = new MappingAction();
                action.setActionClassName(defaultActionClass);
                mappingEndpoint.setAction(action);
            }
        }
    }

    static void parseScopeTag(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String scope = tagMap.get(APIFEST_SCOPE);
        if (scope != null) {
            mappingEndpoint.setScope(scope);
            mappingEndpointDocumentation.setScope(scope);
        }
    }

    static void parseDocsDescriptiveTags(Map<String, String> tagMap, MappingEndpointDocumentation mappingEndpointDocumentation) {
        String docsDescription = tagMap.get(APIFEST_DOCS_DESCRIPTION);
        if (docsDescription != null) {
            mappingEndpointDocumentation.setDescription(docsDescription);
            String[] sentences = docsDescription.split("[.?!]");
            if (sentences.length>0)
            {
                mappingEndpointDocumentation.setSummary(sentences[0]);
            }
        }
        String docsSummary = tagMap.get(APIFEST_DOCS_SUMMARY);
        if (docsSummary != null) {
            mappingEndpointDocumentation.setSummary(docsSummary);
        }
        String docsGroup = tagMap.get(APIFEST_DOCS_GROUP);
        if (docsGroup != null) {
            mappingEndpointDocumentation.setGroup(docsGroup);
        }
        String order = tagMap.get(APIFEST_DOCS_ORDER);
        if (order != null) {
            try{
                mappingEndpointDocumentation.setOrder(Integer.parseInt(order));
            } catch(NumberFormatException nfe) {
                mappingEndpointDocumentation.setOrder(Integer.MAX_VALUE);
            }
        } else
        {
            mappingEndpointDocumentation.setOrder(Integer.MAX_VALUE);
        }
        String exampleRequest = tagMap.get(APIFEST_DOCS_EXAMPLE_REQUEST);
        if (exampleRequest != null) {
            mappingEndpointDocumentation.setExampleRequest(exampleRequest);
        }
        String exampleResult = tagMap.get(APIFEST_DOCS_EXAMPLE_RESULT);
        if (exampleRequest != null) {
            mappingEndpointDocumentation.setExampleResult(exampleResult);
        }

    }

    static void parseHidden(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation) {
        boolean isHidden = tagMap.containsKey(APIFEST_HIDDEN);
        mappingEndpoint.setHidden(isHidden);
        isHidden = tagMap.containsKey(APIFEST_DOCS_HIDDEN);
        mappingEndpointDocumentation.setHidden(isHidden);
    }

    static void parseInternalEndpointTag(Map<String, String> tagMap, MappingEndpoint mappingEndpoint, MappingEndpointDocumentation mappingEndpointDocumentation, String applicationPath) {
        String internalEndpoint = tagMap.get(APIFEST_INTERNAL);
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
                String varExpression = tagMap.get(APIFEST_RE + varName);
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
            mappingEndpointDocumentation.setVarExpression(mappingEndpoint.getVarExpression());
            mappingEndpointDocumentation.setVarName(mappingEndpoint.getVarName());
        }
    }

    static void parseRequestParams(Map<String, String> tagMap,
            MappingEndpointDocumentation mappingEndpointDocumentation)
    {
        String parametersDescription = tagMap.get(APIFEST_PARAMS_DESCRIPTION);
        mappingEndpointDocumentation.setParamsDescription(parametersDescription);
        List<RequestParamDocumentation> paramsList = new ArrayList<RequestParamDocumentation>();
        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Matcher m = PARAM_PATTERN.matcher(key);
            if (!m.matches()){
                continue;
            }
            String name = m.group(1);
            RequestParamDocumentation paramDocumentation = new RequestParamDocumentation();
            if (tagMap.containsKey(REQUEST_PARAMS_PREFIX + name + ".name")){
                paramDocumentation.setName(tagMap.get(REQUEST_PARAMS_PREFIX + name + ".name"));
            }
            else {
                paramDocumentation.setName(name);
            }
            paramDocumentation.setDescription(value);
            paramDocumentation.setType(tagMap.get(REQUEST_PARAMS_PREFIX + name + ".type"));
            paramDocumentation.setRequired(!tagMap.containsKey(REQUEST_PARAMS_PREFIX + name + ".optional"));
            paramDocumentation.setExampleValue(tagMap.get(REQUEST_PARAMS_PREFIX + name + ".exampleValue"));
            paramsList.add(paramDocumentation);
        }

        mappingEndpointDocumentation.setRequestParamsDocumentation(paramsList);
    }

    static void parseResultParams(Map<String, String> tagMap,
            MappingEndpointDocumentation mappingEndpointDocumentation)
    {
        String parametersDescription = tagMap.get(APIFEST_RESULT_PARAMS_DESCRIPTION);
        mappingEndpointDocumentation.setResultsDescription(parametersDescription);
        List<ResultParamDocumentation> paramsList = new ArrayList<ResultParamDocumentation>();
        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Matcher m = RESULT_PARAM_PATTERN.matcher(key);
            if (!m.matches()){
                continue;
            }
            String name = m.group(1);
            ResultParamDocumentation paramDocumentation = new ResultParamDocumentation();
            if (tagMap.containsKey(RESULT_PARAMS_PREFIX + name + ".name")){
                paramDocumentation.setName(tagMap.get(RESULT_PARAMS_PREFIX + name + ".name"));
            }
            else {
                paramDocumentation.setName(name);
            }
            paramDocumentation.setDescription(value);
            paramDocumentation.setType(tagMap.get(RESULT_PARAMS_PREFIX + name + ".type"));
            paramDocumentation.setRequired(!tagMap.containsKey(RESULT_PARAMS_PREFIX + name + ".optional"));
            paramsList.add(paramDocumentation);
        }
        mappingEndpointDocumentation.setResultParamsDocumentation(paramsList);
    }

    static void parseExceptions(final Map<String, String> tagMap,
            MappingEndpointDocumentation mappingEndpointDocumentation)
    {
        final List<ExceptionDocumentation> exceptionsList = new ArrayList<ExceptionDocumentation>();
        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Matcher m = EXCEPTION_PATTERN.matcher(key);
            if (!m.matches()){
                continue;
            }
            String name = m.group(1);
            ExceptionDocumentation exception = new ExceptionDocumentation();
            exception.setName(name);
            exception.setCondition(value);
            exception.setDescription(tagMap.get("apifest.docs.exceptions." + name + ".description"));
            try {
                exception.setCode(Integer.parseInt(tagMap.get("apifest.docs.exceptions." + name + ".code")));
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid code given for exception " + name + ":" + tagMap.get("apifest.docs.exceptions." + name + ".code"));
            }
            exceptionsList.add(exception);
        }
        mappingEndpointDocumentation.setExceptionsDocumentation(exceptionsList);
    }
}
