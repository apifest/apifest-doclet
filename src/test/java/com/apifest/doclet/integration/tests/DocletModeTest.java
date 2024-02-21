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

import com.apifest.doclet.Doclet;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class DocletModeTest {
    private void setMode(String mode) {
        System.setProperty("mode", mode);
    }

    private void runDoclet(String mode) {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/TestParsingResource.java";
        String[] args = new String[] { filePath };
        setMode(mode);
        Doclet.main(args);
    }

    private void findFile(String directory, String name, final String extension) {
        File dir = new File(directory);

        File[] files = dir.listFiles((directory1, fileName) -> fileName.startsWith("all") && fileName.endsWith(extension));
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
    public void when_set_doclet_doc_mode() {
        // WHEN
        runDoclet("doc");
        // Then
        String name = "all-mappings-docs.json";
        String directory = "./";
        findFile(directory, name, ".json");
    }

    @Test
    public void when_set_doclet_mapping_mode() {
        // WHEN
        runDoclet("mapping");
        // Then
        String name = "all-mappings.xml";
        String directory = "./";
        findFile(directory, name, ".xml");
    }
}
