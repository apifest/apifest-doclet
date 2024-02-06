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

package com.apifest.doclet.integration.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.apifest.doclet.Doclet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.apifest.api.params.ExceptionDocumentation;
import com.apifest.api.params.RequestParamDocumentation;
import com.apifest.api.params.ResultParamDocumentation;

public class DocletTest {

    private void deleteJsonFile(String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
    }

    private void runDoclet() {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/TestParsingResource.java";
        String[] args = new String[] { filePath };
        Doclet.main(args);
    }

    private void addRequestParamToMap(String name, String type, String description, boolean required, Map<String, RequestParamDocumentation> correctNameToTypeMap) {
        RequestParamDocumentation tempReqParamDoc = new RequestParamDocumentation();
        tempReqParamDoc.setName(name);
        tempReqParamDoc.setType(type);
        tempReqParamDoc.setDescription(description);
        tempReqParamDoc.setRequired(required);
        correctNameToTypeMap.put(name, tempReqParamDoc);
    }

    private void addResultParamToMap(String name, String type, String description, boolean required, Map<String, ResultParamDocumentation> resNameToTypeMap) {
        ResultParamDocumentation tempResParamDoc = new ResultParamDocumentation();
        tempResParamDoc.setName(name);
        tempResParamDoc.setType(type);
        tempResParamDoc.setDescription(description);
        tempResParamDoc.setRequired(required);
        resNameToTypeMap.put(name, tempResParamDoc);
    }

    private void addException(String name, String description, Integer code, String condition, Map<String, ExceptionDocumentation> exsNameToDescriptionMap) {
        ExceptionDocumentation tempExsParamDoc = new ExceptionDocumentation();
        tempExsParamDoc.setName(name);
        tempExsParamDoc.setDescription(description);
        tempExsParamDoc.setCode(code);
        tempExsParamDoc.setCondition(condition);
        exsNameToDescriptionMap.put(name, tempExsParamDoc);
    }

    @Test
    public void when_doclet_run_outputs_tags() throws IOException {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            String version = rootNode.get("version").asText();
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode obj1 = endpoints.get(0);

            Assert.assertEquals(version, "v1");

            Assert.assertEquals(obj.get("group").asText(), "Twitter Followers");
            Assert.assertEquals(obj.get("scope").asText(), "twitter_followers");
            Assert.assertEquals(obj.get("method").asText(), "GET");
            Assert.assertEquals(obj.get("endpoint").asText(), "/v1/twitter/followers/metrics");
            Assert.assertTrue(obj.get("description").isNull());
            Assert.assertTrue(obj.get("summary").isNull());
            Assert.assertTrue(obj.get("paramsDescription").isNull());
            Assert.assertTrue(obj.get("resultsDescription").isNull());

            Assert.assertEquals(obj1.get("group").asText(), "Twitter Followers");
            Assert.assertEquals(obj1.get("scope").asText(), "twitter_followers");
            Assert.assertEquals(obj1.get("method").asText(), "GET");
            Assert.assertEquals(obj1.get("endpoint").asText(), "/v1/twitter/followers/stream");
            Assert.assertNotEquals(obj1.get("description").asText(), null);
            Assert.assertNotEquals(obj1.get("summary").asText(), null);
            Assert.assertEquals(obj1.get("paramsDescription").asText(), "** Parameter description is going here!**");
            Assert.assertEquals(obj1.get("resultsDescription").asText(), "** Result description is the best! **");
            Assert.assertEquals(obj.get("varExpression").asText(), "\\w[\\w\\s%]+(?<!\\s)");
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void when_doclet_run_generate_customAnnotations() throws IOException {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode customProperties = obj.get("customProperties");
            Assert.assertEquals(customProperties.get("com.apifest.doclet.tests.resources.CustomAnnotation.value").asText(), "test,test2");
            Assert.assertEquals(customProperties.get("com.apifest.doclet.tests.resources.Multiple.names").asText(), "test,test2");
            Assert.assertEquals(customProperties.get("com.apifest.doclet.tests.resources.Multiple.value").asText(), "2,1");
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_whether_json_file_will_generate_unsupported_tags() throws IOException {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode obj1 = endpoints.get(0);
            Assert.assertNull(obj.get("test"));
            Assert.assertNull(obj1.get("test1"));
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_when_tags_are_wrong() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            Assert.assertNull(obj.get("wrongtag"));
            Assert.assertNull(obj.get("@wrongtag"));
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_metrics_request_parameters() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, RequestParamDocumentation> correctNameToTypeMap = new HashMap<String, RequestParamDocumentation>();
        addRequestParamToMap("ids", "string", "** user ids goes here **", true, correctNameToTypeMap);
        addRequestParamToMap("fields", "list", "** The keys from result json can be added as filter**", false, correctNameToTypeMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode requestParams = obj.get("requestParams");
            for (JsonNode requestParam : requestParams) {
                String currentName = requestParam.get("name").asText();
                RequestParamDocumentation correctCurrentParam = correctNameToTypeMap.get(currentName);
                Assert.assertEquals(requestParam.get("type").asText(), correctCurrentParam.getType());
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(requestParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(requestParam.get("required").asBoolean(), correctCurrentParam.isRequired());
            }

        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_stream_request_parameters() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, RequestParamDocumentation> correctNameToTypeMap = new HashMap<String, RequestParamDocumentation>();
        addRequestParamToMap("ids", "string", "** user ids goes here **", true, correctNameToTypeMap);
        addRequestParamToMap("fields", "list", "** The keys from result json can be added as filter**", false, correctNameToTypeMap);
        addRequestParamToMap("since", "integer", "** since is optional parameter**", false, correctNameToTypeMap);
        addRequestParamToMap("until", "integer", "** until is optional parameter**", false, correctNameToTypeMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode requestParams = obj.get("requestParams");
            for (JsonNode requestParam : requestParams) {
                String currentName = requestParam.get("name").asText();
                RequestParamDocumentation correctCurrentParam = correctNameToTypeMap.get(currentName);
                Assert.assertEquals(requestParam.get("type").asText(), correctCurrentParam.getType());
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(requestParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(requestParam.get("required").asBoolean(), correctCurrentParam.isRequired());
            }
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_metrics_result_parameters() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, ResultParamDocumentation> resNameToTypeMap = new HashMap<String, ResultParamDocumentation>();
        addResultParamToMap("channel", "string", "The **channel** description", true, resNameToTypeMap);
        addResultParamToMap("updated_time", "string", "The **updated_time** description", true, resNameToTypeMap);
        addResultParamToMap("request_handle", "string", "The **request_handle** description", true, resNameToTypeMap);
        addResultParamToMap("sentiment.score", "string", "The **sentiment_score** description", true, resNameToTypeMap);
        addResultParamToMap("sentiment.positive", "string", "The **sentiment_positive** description", true, resNameToTypeMap);
        addResultParamToMap("sentiment.neutral", "string", "The **sentiment_neutral** description", true, resNameToTypeMap);
        addResultParamToMap("sentiment.negative", "string", "The **sentiment_negative** description", true, resNameToTypeMap);
        addResultParamToMap("engagement.replies", "integer", "The **engagement_replies** description", true, resNameToTypeMap);
        addResultParamToMap("engagement.tweets", "integer", "The **engagement_tweets** description", true, resNameToTypeMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode resultParams = obj.get("resultParams");
            for (JsonNode resultParam : resultParams) {
                String currentName = resultParam.get("name").asText();
                ResultParamDocumentation correctCurrentParam = resNameToTypeMap.get(currentName);
                Assert.assertEquals(resultParam.get("type").asText(), correctCurrentParam.getType());
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(resultParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(resultParam.get("required").asBoolean(), correctCurrentParam.isRequired());

            }
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_stream_result_parameters() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, ResultParamDocumentation> resNameToTypeMap = new HashMap<String, ResultParamDocumentation>();
        addResultParamToMap("tw_id", "integer", "The ** tw_id ** description", true, resNameToTypeMap);
        addResultParamToMap("in_reply_to_screen_name", "string", "The ** in_reply_to_screen_name ** description", true, resNameToTypeMap);
        addResultParamToMap("request_handle", "string", "The ** request_handle ** description", true, resNameToTypeMap);
        addResultParamToMap("in_reply_to_status_id", "string", "The ** in_reply_to_status_id ** description", true, resNameToTypeMap);
        addResultParamToMap("channel", "string", "The ** channel ** description", true, resNameToTypeMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(0);
            JsonNode resultParams = obj.get("resultParams");
            for (JsonNode resultParam : resultParams) {
                String currentName = resultParam.get("name").asText();
                ResultParamDocumentation correctCurrentParam = resNameToTypeMap.get(currentName);
                Assert.assertEquals(resultParam.get("type").asText(), correctCurrentParam.getType());
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(resultParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(resultParam.get("required").asBoolean(), correctCurrentParam.isRequired());
            }
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_metric_exceptions() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, ExceptionDocumentation> exsNameToDescriptionMap = new HashMap<String, ExceptionDocumentation>();
        addException("invalid_parameter", "Please add valid parameter", 400, "The parameter is invalid", exsNameToDescriptionMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(1);
            JsonNode exceptionsParams = obj.get("exceptions");
            for (JsonNode exceptionParam : exceptionsParams) {
                String currentName = exceptionParam.get("name").asText();
                ExceptionDocumentation correctCurrentParam = exsNameToDescriptionMap.get(currentName);
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(exceptionParam.get("condition").asText(), correctCurrentParam.getCondition());
                Assert.assertEquals(exceptionParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(exceptionParam.get("code").asLong(), Long.valueOf(correctCurrentParam.getCode()).longValue());
            }
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_stream_exceptions() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        Map<String, ExceptionDocumentation> exsNameToDescriptionMap = new HashMap<String, ExceptionDocumentation>();
        addException("invalid_since_until", "Since/until parameter must be within the last 30 days", 400, "The period is invalid", exsNameToDescriptionMap);
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(0);
            JsonNode exceptionsParams = obj.get("exceptions");
            for (JsonNode exceptionParam : exceptionsParams) {
                String currentName = exceptionParam.get("name").asText();
                ExceptionDocumentation correctCurrentParam = exsNameToDescriptionMap.get(currentName);
                Assert.assertEquals(currentName, correctCurrentParam.getName());
                Assert.assertEquals(exceptionParam.get("condition").asText(), correctCurrentParam.getCondition());
                Assert.assertEquals(exceptionParam.get("description").asText(), correctCurrentParam.getDescription());
                Assert.assertEquals(exceptionParam.get("code").asLong(), Long.valueOf(correctCurrentParam.getCode()).longValue());
            }
        } finally {
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_what_doclet_will_generate_correct_endpoints_order() throws Exception {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader fileReader = new FileReader(parserFilePath)) {
            JsonNode rootNode = objectMapper.readTree(fileReader);
            ArrayNode endpoints = (ArrayNode) rootNode.get("endpoints");
            JsonNode obj = endpoints.get(0);
            JsonNode obj1 = endpoints.get(1);
            Assert.assertEquals(obj.get("endpoint").asText(), "/v1/twitter/followers/stream");
            Assert.assertEquals(obj1.get("endpoint").asText(), "/v1/twitter/followers/metrics");

        } finally {
            deleteJsonFile(parserFilePath);
        }
    }
}
