package com.apifest.doclet.tests.resources;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)//TODO: check
@Documented
public @interface Multiple {
    int[] value();
    String[] names();
}
