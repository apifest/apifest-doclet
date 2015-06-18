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

package com.apifest.doclet.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.doclet.Doclet;

public class DocletModeTest {
    @BeforeMethod
    public void setup() {
        System.setProperty("sourcePath", "./src/test/java/com/apifest/doclet/tests/resources");
        System.setProperty("mapping.version", "v1");
        System.setProperty("mapping.filename", "all-mappings.xml");
        System.setProperty("mapping.docs.filename", "all-mappings-docs.json");
        System.setProperty("backend.host", "localhost");
        System.setProperty("backend.port", "1212");
        System.setProperty("application.path", "/");
        System.setProperty("defaultActionClass", "com.all.mappings.DefaultMapping");
        System.setProperty("defaultFilterClass", "com.all.mappings.DefaultFilter");

    }

    private void setMode(String mode) {
        System.setProperty("mode", mode);
    }

    private void runDoclet(String mode) {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/TestParsingResource.java";
        Doclet doclet = new Doclet();
        String[] args = new String[] { filePath };
        setMode(mode);
        Doclet.main(args);
    }

    private void findFile(String directory, String name, final String extension) {
        File dir = new File(directory);

        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                if (fileName.startsWith("all") && fileName.endsWith(extension)) {
                    return true;
                }
                return false;
            }
        });
        for (File f : files) {
            Assert.assertEquals(name, f.getName());
            f.deleteOnExit();
        }
    }

    @AfterMethod
    public void clearProperties() {
        System.clearProperty("propertiesFilePath");
    }

    @Test
    public void when_set_doclet_doc_mode() throws IOException, ParseException {
        // WHEN
        runDoclet("doc");
        // Then
        String name = "all-mappings-docs.json";
        String directory = "./";
        findFile(directory, name, ".json");
    }

    @Test
    public void when_set_doclet_mapping_mode() throws IOException, ParseException {
        // WHEN
        runDoclet("mapping");
        // Then
        String name = "all-mappings.xml";
        String directory = "./";
        findFile(directory, name, ".xml");
    }
}
