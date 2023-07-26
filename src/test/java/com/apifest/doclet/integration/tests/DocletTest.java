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

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.apifest.doclet.Doclet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.api.params.ExceptionDocumentation;
import com.apifest.api.params.RequestParamDocumentation;
import com.apifest.api.params.ResultParamDocumentation;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

public class DocletTest {
    @BeforeMethod
    public void setup() {
        System.setProperty("sourcePath", "./src/test/java/com/apifest/doclet/tests/resources");
        System.setProperty("mode", "doc");
        System.setProperty("mapping.version", "v1");
        System.setProperty("mapping.filename", "all-mappings.xml");
        System.setProperty("mapping.docs.filename", "all-mappings-docs.json");
        System.setProperty("backend.host", "localhost");
        System.setProperty("backend.port", "1212");
        System.setProperty("application.path", "/");
        System.setProperty("defaultActionClass", "com.all.mappings.DefaultMapping");
        System.setProperty("defaultFilterClass", "com.all.mappings.DefaultFilter");
    }

    @Test
    public void testMain() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));

        String[] args = {
                "-doclet", "com.apifest.doclet.Doclet",
                "-docletpath", "./target/apifest-doclet-0.1.2-SNAPSHOT.jar",
                "-sourcepath", "./src/test/java",
                "-cp", "./target/apifest-doclet-0.1.2-SNAPSHOT.jar",
                "-mode", "doc,mapping",
                "-mappingVersion", "v1",
                "-mappingFilename", "all-mappings.xml",
                "-mappingDocsFilename", "all-mappings-docs.json",
                "-backendHost", "localhost",
                "-backendPort", "1212",
                "-applicationPath", "/",
                "-defaultActionClass", "com.all.mappings.DefaultMapping",
                "-defaultFilterClass", "com.all.mappings.DefaultFilter",
//                "-J-Xdebug",
//                "-J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:5005",
                "com.apifest.doclet.tests.resources"
        };

        try {
            Doclet.main(args);
        } catch (RuntimeException e) {
            fail("Exception thrown during test: " + e.toString());
        }

        // Restore System.out
        System.setOut(originalOut);

        String output = new String(bos.toByteArray());
        System.out.println(output);
    }

    private void deleteJsonFile(String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
    }

    private void runDoclet() {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/TestParsingResource.java";
        Doclet doclet = new Doclet();
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

    @AfterMethod
    public void clearProperties() {
        System.clearProperty("propertiesFilePath");
    }

    @Test
    public void when_doclet_run_outputs_tags() throws ParseException, IOException {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            String version = (String) json.get("version");
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(1);
            JSONObject obj1 = (JSONObject) arr.get(0);

            Assert.assertEquals(version, "v1");

            Assert.assertEquals(obj.get("group"), "Twitter Followers");
            Assert.assertEquals(obj.get("scope"), "twitter_followers");
            Assert.assertEquals(obj.get("method"), "GET");
            Assert.assertEquals(obj.get("endpoint"), "/v1/twitter/followers/metrics");
            Assert.assertEquals(obj.get("description"), null);
            Assert.assertEquals(obj.get("summary"), null);
            Assert.assertEquals(obj.get("paramsDescription"), null);
            Assert.assertEquals(obj.get("resultsDescription"), null);

            Assert.assertEquals(obj1.get("group"), "Twitter Followers");
            Assert.assertEquals(obj1.get("scope"), "twitter_followers");
            Assert.assertEquals(obj1.get("method"), "GET");
            Assert.assertEquals(obj1.get("endpoint"), "/v1/twitter/followers/stream");
            Assert.assertNotEquals(obj1.get("description"), null);
            Assert.assertNotEquals(obj1.get("summary"), null);
            Assert.assertEquals(obj1.get("paramsDescription"), "** Parameter description is going here!**");
            Assert.assertEquals(obj1.get("resultsDescription"), "** Result description is the best! **");
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
            deleteJsonFile(parserFilePath);
        }
    }

    @Test
    public void check_whether_json_file_will_generate_unsupported_tags() throws IOException, ParseException {
        // GIVEN
        String parserFilePath = "./all-mappings-docs.json";
        // WHEN
        runDoclet();
        // THEN
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);
            JSONObject obj1 = (JSONObject) arr.get(1);
            Assert.assertEquals(obj.get("test"), null);
            Assert.assertEquals(obj1.get("test1"), null);
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);
            Assert.assertEquals(obj.get("wrongtag"), null);
            Assert.assertEquals(obj.get("@wrongtag"), null);
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(1);

            JSONArray reqParam = (JSONArray) obj.get("requestParams");

            for (int i = 0; i < reqParam.size(); i++) {
                JSONObject currentParam = (JSONObject) reqParam.get(i);
                String currentName = (String) currentParam.get("name");
                RequestParamDocumentation correctCurrentParam = correctNameToTypeMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("type"), correctCurrentParam.getType());
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("required"), correctCurrentParam.isRequired());
                // System.out.println(currentParam.get("name"));
                // System.out.println(correctCurrentParam.getName());
            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);

            JSONArray reqParam = (JSONArray) obj.get("requestParams");

            for (int i = 0; i < reqParam.size(); i++) {
                JSONObject currentParam = (JSONObject) reqParam.get(i);
                String currentName = (String) currentParam.get("name");
                RequestParamDocumentation correctCurrentParam = correctNameToTypeMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("type"), correctCurrentParam.getType());
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("required"), correctCurrentParam.isRequired());
            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(1);

            JSONArray resParam = (JSONArray) obj.get("resultParams");
            for (int i = 0; i < resParam.size(); i++) {
                JSONObject currentParam = (JSONObject) resParam.get(i);
                String currentName = (String) currentParam.get("name");
                ResultParamDocumentation correctCurrentParam = resNameToTypeMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("type"), correctCurrentParam.getType());
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("required"), correctCurrentParam.isRequired());

            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);

            JSONArray resParam = (JSONArray) obj.get("resultParams");
            for (int i = 0; i < resParam.size(); i++) {
                JSONObject currentParam = (JSONObject) resParam.get(i);
                String currentName = (String) currentParam.get("name");
                ResultParamDocumentation correctCurrentParam = resNameToTypeMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("type"), correctCurrentParam.getType());
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("required"), correctCurrentParam.isRequired());
            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(1);

            JSONArray exsParam = (JSONArray) obj.get("exceptions");
            for (int i = 0; i < exsParam.size(); i++) {
                JSONObject currentParam = (JSONObject) exsParam.get(i);
                String currentName = (String) currentParam.get("name");
                ExceptionDocumentation correctCurrentParam = exsNameToDescriptionMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("condition"), correctCurrentParam.getCondition());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("code"), Long.valueOf(correctCurrentParam.getCode()));
            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);

            JSONArray exsParam = (JSONArray) obj.get("exceptions");
            for (int i = 0; i < exsParam.size(); i++) {
                JSONObject currentParam = (JSONObject) exsParam.get(i);
                String currentName = (String) currentParam.get("name");
                ExceptionDocumentation correctCurrentParam = exsNameToDescriptionMap.get(currentName);
                Assert.assertEquals((String) currentParam.get("name"), correctCurrentParam.getName());
                Assert.assertEquals((String) currentParam.get("condition"), correctCurrentParam.getCondition());
                Assert.assertEquals((String) currentParam.get("description"), correctCurrentParam.getDescription());
                Assert.assertEquals(currentParam.get("code"), Long.valueOf(correctCurrentParam.getCode()));
            }
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
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
        JSONParser parser = new JSONParser();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(parserFilePath);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray arr = (JSONArray) json.get("endpoints");
            JSONObject obj = (JSONObject) arr.get(0);
            JSONObject obj1 = (JSONObject) arr.get(1);
            Assert.assertEquals(obj.get("endpoint"), "/v1/twitter/followers/stream");
            Assert.assertEquals(obj1.get("endpoint"), "/v1/twitter/followers/metrics");

        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
            deleteJsonFile(parserFilePath);
        }

    }
}
