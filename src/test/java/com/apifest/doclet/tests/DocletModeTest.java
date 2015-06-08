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
        System.setProperty("propertiesFilePath", "./src/test/java/com/apifest/doclet/tests/resources/project.properties");
        System.setProperty("mapping.version", "v1");
        System.setProperty("mapping.filename", "komfo-mappings.xml");
        System.setProperty("mapping.docs.filename", "komfo-mappings-docs.json");
        System.setProperty("backend.host", "localhost");
        System.setProperty("backend.port", "1212");
        System.setProperty("application.path", "/");
        System.setProperty("defaultActionClass", "com.komfo.mappings.DefaultMapping");
        System.setProperty("defaultFilterClass", "com.komfo.mappings.DefaultFilter");

    }

    private void setMode(String mode) {
        System.setProperty("mode", mode);
    }

    private void runDoclet(String mode) {
        String filePath = "./src/test/java/com/apifest/doclet/tests/resources/ParsingResource.java";
        Doclet doclet = new Doclet();
        String[] args = new String[] { filePath };
        setMode(mode);
        Doclet.main(args, false);
    }

    private void findFile(String directory, String name, final String extension) {
        File dir = new File(directory);

        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                if (fileName.startsWith("komfo") && fileName.endsWith(extension)) {
                    return true;
                }
                return false;
            }
        });
        for (File f : files) {
            System.out.println(f.getName());
            System.out.println("==============");
            System.out.println(name);
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
        // GIVEN
        // setMode("doc");
        // WHEN
        runDoclet("doc");
        // Then
        String name = "komfo-mappings-docs.json";
        String directory = "./";
        findFile(directory, name, ".json");
    }

    @Test
    public void when_set_doclet_mapping_mode() throws IOException, ParseException {
        // GIVEN
        // setMode("mapping");
        // WHEN
        runDoclet("mapping");
        // Then
        String name = "komfo-mappings.xml";
        String directory = "./";
        findFile(directory, name, ".xml");
    }

}
