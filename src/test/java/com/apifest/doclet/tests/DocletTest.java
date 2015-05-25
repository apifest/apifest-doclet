package com.apifest.doclet.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.doclet.Doclet;

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
        System.setProperty(" application.path", "/");
        System.setProperty("defaultActionClass", "com.all.mappings.DefaultMapping");
        System.setProperty("defaultFilterClass", "com.all.mappings.DefaultFilter");
    }

    private void deleteJsonFile(String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
    }

    private void runDoclet() {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/ParsingResource.java";
        Doclet doclet = new Doclet();
        String[] args = new String[] { filePath };
        Doclet.main(args);

    }

    @AfterMethod
    public void clearProperties() {
        System.clearProperty("propertiesFilePath");
    }

    @Test
    public void when_doclet_run_outputs_tags() throws IOException, ParseException {
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
            JSONObject obj = (JSONObject) arr.get(0);
            JSONObject obj1 = (JSONObject) arr.get(1);

            Assert.assertEquals(version, "v1");

            Assert.assertEquals(obj.get("group"), "Twitter Followers");
            Assert.assertEquals(obj.get("scope"), "twitter_followers");
            Assert.assertEquals(obj.get("method"), "GET");
            Assert.assertEquals(obj.get("endpoint"), "/v1/twitter/followers/metrics");
            Assert.assertEquals(obj.get("description"), null);
            Assert.assertEquals(obj.get("summary"), null);

            Assert.assertEquals(obj1.get("group"), "Twitter Followers");
            Assert.assertEquals(obj1.get("scope"), "twitter_followers");
            Assert.assertEquals(obj1.get("method"), "GET");
            Assert.assertEquals(obj1.get("endpoint"), "/v1/twitter/followers/stream");
            Assert.assertNotEquals(obj1.get("description"), null);
            Assert.assertNotEquals(obj1.get("summary"), null);
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
}
