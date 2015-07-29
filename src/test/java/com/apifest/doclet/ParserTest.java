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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;
import com.apifest.api.params.ExceptionDocumentation;
import com.apifest.api.params.RequestParamDocumentation;
import com.apifest.api.params.ResultParamDocumentation;

public class ParserTest {
    private static final String APIFEST_DOCS_PARAMS_PREFIX = "apifest.docs.params.";
    private static final String APIFEST_DOCS_RESULT_PREFIX = "apifest.docs.results.";
    private static final String APIFEST_DOCS_EXCEPTIONS_PREFIX = "apifest.docs.exceptions.";
    private Map<String, String> tagMap;
    private MappingEndpoint mappingEndpoint;
    private MappingEndpointDocumentation mappingEndpointDocumentation;

    @BeforeMethod
    public void setup() {
        tagMap = new HashMap<String, String>();
        tagMap.put("apifest.external", "/ads/customaudiences/{audienceId}/status");
        tagMap.put("apifest.internal", "{platform}/{clientId}/ads/customaudiences/{audienceId}/status     ");
        tagMap.put("apifest.scope", "advertising");
        tagMap.put("apifest.auth.type", "client-app");
        tagMap.put("apifest.re.audienceId", "\\d+");
        tagMap.put("apifest.docs.group", "Custom Audiences");
        tagMap.put("apifest.docs.summary", "Get statistics about a Custom Audience.");
        tagMap.put(
                "apifest.docs.description",
                "Use this endpoint to get statistics about a Custom Audience. The most     important field is the approximate_size - the the number of facebook users for which Facebook     found a match against the users you have uploaded.");
        tagMap.put("apifest.docs.exampleRequest", "Example request");
        tagMap.put("apifest.docs.exampleResult", "Example result");
        tagMap.put("apifest.docs.paramsDescription", " Parameter description yo!");
        tagMap.put("apifest.docs.params.audienceId", " audienceId is compulsory");
        tagMap.put("apifest.docs.params.audienceId.type", "number");
        tagMap.put("apifest.docs.params.audienceId.default", "There is no default because it is compulsory     ");
        tagMap.put("apifest.docs.params.audienceId.exampleValue", "12345678");

        tagMap.put("apifest.docs.params.audienceId2", " audienceId is compulsory");
        tagMap.put("apifest.docs.params.audienceId2.type", "number");
        tagMap.put("apifest.docs.params.audienceId2.default", " 7nadesetipolovina");
        tagMap.put("apifest.docs.params.audienceId2.optional", "");
        tagMap.put("apifest.docs.resultsDescription", " Result description is the best!");
        tagMap.put("apifest.docs.results.approximate_count", " The  approximate  number of accounts recognised by fb in the CA     ");
        tagMap.put("apifest.docs.results.approximate_count.type", "number");
        tagMap.put("apifest.docs.results.approximate_count2", " The  approximate  number of accounts recognised by fb in the CA     ");
        tagMap.put("apifest.docs.results.approximate_count2.type", "number");
        tagMap.put("apifest.docs.results.approximate_count2.optional", "");
        tagMap.put("apifest.docs.exceptions.no_such_audience", "The Custom Audience does not exist or you don't have access to it.");
        tagMap.put("apifest.docs.exceptions.no_such_audience.description", "The Custom Audience does not exist or you don't have access to it.");
        tagMap.put("apifest.docs.exceptions.no_such_audience.code", "404");
        mappingEndpoint = new MappingEndpoint();
        mappingEndpointDocumentation = new MappingEndpointDocumentation();
    }

    @Test
    public void testParseScopeCorrect() {
        Parser.parseScopeTag(tagMap, mappingEndpoint, mappingEndpointDocumentation);
        Assert.assertEquals(mappingEndpoint.getScope(), tagMap.get("apifest.scope"));
        Assert.assertEquals(mappingEndpointDocumentation.getScope(), tagMap.get("apifest.scope"));
    }

    @Test
    public void test_parse_action_tag_correct() {
        String defaultActionClass = "testAction";
        Parser.parseActionTag(tagMap, mappingEndpoint, defaultActionClass);
        Assert.assertEquals(mappingEndpoint.getAction().getActionClassName(), defaultActionClass);
        tagMap.put("apifest.action", "Arnold");
        Parser.parseActionTag(tagMap, mappingEndpoint, defaultActionClass);
        Assert.assertEquals(mappingEndpoint.getAction().getActionClassName(), "Arnold");
    }

    @Test
    public void test_parse_auth_tag_correct() throws ParseException {
        Parser.parseAuthTypeTag(tagMap, mappingEndpoint);
        Assert.assertEquals(mappingEndpoint.getAuthType(), tagMap.get("apifest.auth.type"));
    }

    @Test
    public void test_parse_docs_description_correct() {
        Parser.parseDocsDescriptiveTags(tagMap, mappingEndpointDocumentation);
        Assert.assertEquals(mappingEndpointDocumentation.getDescription(), tagMap.get("apifest.docs.description"));
        Assert.assertEquals(mappingEndpointDocumentation.getGroup(), tagMap.get("apifest.docs.group"));
        Assert.assertEquals(mappingEndpointDocumentation.getSummary(), tagMap.get("apifest.docs.summary"));
        Assert.assertEquals(mappingEndpointDocumentation.getExampleRequest(), tagMap.get("apifest.docs.exampleRequest"));
        Assert.assertEquals(mappingEndpointDocumentation.getExampleResult(), tagMap.get("apifest.docs.exampleResult"));
    }

    @Test
    public void test_parse_summary_extrapolation_correct() {
        tagMap.remove("apifest.docs.summary");
        Parser.parseDocsDescriptiveTags(tagMap, mappingEndpointDocumentation);
        Assert.assertTrue(tagMap.get("apifest.docs.description").startsWith(mappingEndpointDocumentation.getSummary()));
    }

    @Test
    public void test_parse_endpoint_backend_tags_default_correct() {
        Parser.parseEndpointBackendTags(tagMap, mappingEndpoint, "localhost", 1313);
        Assert.assertEquals(mappingEndpoint.getBackendHost(), "localhost");
        Assert.assertEquals(mappingEndpoint.getBackendPort(), new Integer(1313));
    }

    @Test
    public void test_parse_endpoint_backend_tags_correct() {
        tagMap.put("apifest.backend.host", "testHost");
        tagMap.put("apifest.backend.port", "1111");
        Parser.parseEndpointBackendTags(tagMap, mappingEndpoint, "localhost", 1313);
        Assert.assertEquals(mappingEndpoint.getBackendHost(), "testHost");
        Assert.assertEquals(mappingEndpoint.getBackendPort(), new Integer(1111));
    }

    @Test
    public void test_parse_filter_tag_correct() {
        String defaultFilterClass = "defaultFilter";
        Parser.parseFilterTag(tagMap, mappingEndpoint, defaultFilterClass);
        Assert.assertEquals(mappingEndpoint.getFilter().getFilterClassName(), defaultFilterClass);
        tagMap.put("apifest.filter", "Mr.Proper");
        Parser.parseFilterTag(tagMap, mappingEndpoint, defaultFilterClass);
        Assert.assertEquals(mappingEndpoint.getFilter().getFilterClassName(), "Mr.Proper");
    }

    @Test
    public void test_parse_hidden() {
        Parser.parseHidden(tagMap, mappingEndpoint, mappingEndpointDocumentation);
        Assert.assertEquals(mappingEndpoint.isHidden(), false);
        Assert.assertEquals(mappingEndpointDocumentation.isHidden(), false);
        tagMap.put("apifest.hidden", "");
        tagMap.put("apifest.docs.hidden", "");
        Parser.parseHidden(tagMap, mappingEndpoint, mappingEndpointDocumentation);
        Assert.assertEquals(mappingEndpoint.isHidden(), true);
        Assert.assertEquals(mappingEndpointDocumentation.isHidden(), true);
    }

    @Test
    public void test_parse_internal_endpoint_tag_with_empty_application_path() {
        String applicationPath = "";
        String internalEndpoint = "/komfo/test/endpoint";
        tagMap.put("apifest.internal", internalEndpoint);
        Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPath);
        Assert.assertEquals(mappingEndpoint.getInternalEndpoint(), internalEndpoint);
    }

    @Test
    public void test_parse_internal_endpoint_tag_with_not_empty_application_path() {
        String applicationPath = "test_app_path";
        String internalEndpoint = "/komfo/test/endpoint";
        String path = applicationPath + internalEndpoint;
        tagMap.put("apifest.internal", internalEndpoint);
        Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPath);
        Assert.assertEquals(mappingEndpoint.getInternalEndpoint(), path);
    }

    @Test
    public void test_parse_internal_endpoint_tag_with_null_path_parameter_expression() {
        String applicationPath = "test";
        String internalEndpoint = "/komfo/{test}/endpoint";
        tagMap.put("apifest.internal", internalEndpoint);
        Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPath);
        Assert.assertEquals(mappingEndpoint.getVarName(), null);
        Assert.assertEquals(mappingEndpoint.getVarExpression(), null);
    }

    @Test
    public void test_parse_internal_endpoint_tag_with_path_parameter_expression() {
        String applicationPath = "test";
        String internalEndpoint = "/komfo/{test}/endpoint";
        String pathExpression = "abc";
        tagMap.put("apifest.internal", internalEndpoint);
        tagMap.put("apifest.re.test", pathExpression);
        Parser.parseInternalEndpointTag(tagMap, mappingEndpoint, mappingEndpointDocumentation, applicationPath);
        Assert.assertEquals(mappingEndpoint.getVarName(), "test");
        Assert.assertEquals(mappingEndpoint.getVarExpression(), pathExpression);
    }

    @Test
    public void test_parse_request_params() {
        Parser.parseRequestParams(tagMap, mappingEndpointDocumentation);
        List<RequestParamDocumentation> testReqParam = mappingEndpointDocumentation.getRequestParamsDocumentation();
        Assert.assertEquals(mappingEndpointDocumentation.getParamsDescription(), tagMap.get("apifest.docs.paramsDescription"));
        for (int i = 0; i < testReqParam.size(); i++) {
            RequestParamDocumentation currentParam = testReqParam.get(i);
            String currentName = currentParam.getName();
            String optional = tagMap.get(APIFEST_DOCS_PARAMS_PREFIX + currentName + ".optional");
            Assert.assertEquals(currentParam.getType(), tagMap.get(APIFEST_DOCS_PARAMS_PREFIX + currentName + ".type"));
            Assert.assertEquals(currentParam.getDescription(), tagMap.get(APIFEST_DOCS_PARAMS_PREFIX + currentName));
            Assert.assertEquals(currentParam.getExampleValue(), tagMap.get(APIFEST_DOCS_PARAMS_PREFIX + currentName + ".exampleValue"));
            Assert.assertEquals(testReqParam.size(), 2);
            if (optional == null) {
                Assert.assertEquals(currentParam.isRequired(), true);
            } else {
                Assert.assertEquals(currentParam.isRequired(), false);
            }
        }
    }

    @Test
    public void test_parse_set_name_of_request_params() {
        String audienceName = "audience.name";
        tagMap.put("apifest.docs.params.audienceId.name", audienceName);
        Parser.parseRequestParams(tagMap, mappingEndpointDocumentation);
        List<RequestParamDocumentation> testRegParam = mappingEndpointDocumentation.getRequestParamsDocumentation();
        for (int i = 0; i < testRegParam.size(); i++) {
            RequestParamDocumentation currentParam = testRegParam.get(i);
            String currentName = currentParam.getName();
            if (currentName == audienceName) {
                Assert.assertEquals(currentParam.getName(), audienceName);
            } else {
                Assert.assertEquals(currentParam.getName(), "audienceId2");
            }
        }
    }

    @Test
    public void test_parse_result_params() {
        Parser.parseResultParams(tagMap, mappingEndpointDocumentation);
        List<ResultParamDocumentation> testResParam = mappingEndpointDocumentation.getResultParamsDocumentation();
        Assert.assertEquals(mappingEndpointDocumentation.getResultsDescription(), tagMap.get("apifest.docs.resultsDescription"));
        for (int i = 0; i < testResParam.size(); i++) {
            ResultParamDocumentation currentParam = testResParam.get(i);
            String currentName = currentParam.getName();
            String optional = tagMap.get(APIFEST_DOCS_RESULT_PREFIX + currentName + ".optional");
            Assert.assertEquals(currentParam.getType(), tagMap.get(APIFEST_DOCS_RESULT_PREFIX + currentName + ".type"));
            Assert.assertEquals(currentParam.getDescription(), tagMap.get(APIFEST_DOCS_RESULT_PREFIX + currentName));
            Assert.assertEquals(testResParam.size(), 2);
            if (optional == null) {
                Assert.assertEquals(currentParam.isRequired(), true);
            } else {
                Assert.assertEquals(currentParam.isRequired(), false);
            }
        }
    }

    @Test
    public void test_parse_set_name_of_result_params() {
        String approximateCount = "approximate.count";
        tagMap.put("apifest.docs.results.approximate_count.name", approximateCount);
        Parser.parseResultParams(tagMap, mappingEndpointDocumentation);
        List<ResultParamDocumentation> testResParam = mappingEndpointDocumentation.getResultParamsDocumentation();
        for (int i = 0; i < testResParam.size(); i++) {
            ResultParamDocumentation currentParam = testResParam.get(i);
            String currentName = currentParam.getName();
            if (currentName == approximateCount) {
                Assert.assertEquals(currentParam.getName(), approximateCount);
            } else {
                Assert.assertEquals(currentParam.getName(), "approximate_count2");
            }
        }
    }

    @Test
    public void test_parse_exceptions() {
        Parser.parseExceptions(tagMap, mappingEndpointDocumentation);
        List<ExceptionDocumentation> testExsParam = mappingEndpointDocumentation.getExceptionsDocumentation();
        for (int i = 0; i < testExsParam.size(); i++) {
            ExceptionDocumentation currentParam = testExsParam.get(i);
            String currentName = currentParam.getName();
            Assert.assertEquals(currentParam.getCondition(), tagMap.get(APIFEST_DOCS_EXCEPTIONS_PREFIX + currentName));
            Assert.assertEquals(currentParam.getDescription(), tagMap.get(APIFEST_DOCS_EXCEPTIONS_PREFIX + currentName + ".description"));
            Assert.assertEquals(currentParam.getCode().toString(), tagMap.get(APIFEST_DOCS_EXCEPTIONS_PREFIX + currentName + ".code"));
        }

    }

}
