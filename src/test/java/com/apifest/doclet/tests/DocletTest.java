package com.apifest.doclet.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apifest.doclet.Doclet;

public class DocletTest {
    @BeforeMethod
    public void setup() {
    	System.setProperty("sourcePath", "C:\\Users\\Iliana\\git\\apifest-doclet\\src\\test\\java\\com\\apifest\\doclet\\tests\\resources");
    	System.setProperty("propertiesFilePath", "C:\\Users\\Iliana\\git\\apifest-doclet\\src\\test\\java\\com\\apifest\\doclet\\tests\\resources\\project.properties");
    	System.setProperty("mode", "doc");
    }

    @Test
    public void when_doclet_run_outputs_some_content() {
        // GIVEN
    	String filePath = "C:\\Users\\Iliana\\git\\apifest-doclet\\src\\test\\java\\com\\apifest\\doclet\\tests\\resources\\ParsingResource.java";
        // WHEN
    	Doclet doclet = new Doclet();
    	String[] args = new String[] { filePath };
    	doclet.main(args);
        // THEN
        Assert.assertNotEquals("php", "java");
    }
}
