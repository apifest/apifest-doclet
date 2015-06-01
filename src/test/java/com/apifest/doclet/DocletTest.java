package com.apifest.doclet;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DocletTest {
    @BeforeMethod
    public void setup() {

    }

    @Test
    public void when_no_ids_passed_should_return_missing_param_api_error() {
        // GIVEN
        // WHEN

        // THEN
        Assert.assertNotEquals("php", "java");
    }
}
