package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class ApplicationPathOption implements Option {
    private String applicationPath;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the application path";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-applicationPath", "--application-path");
    }

    @Override
    public String getParameters() {
        return "path";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        String value = arguments.get(0);
        if ("NONE".equalsIgnoreCase(value)) {
            return true;
        }
        applicationPath = value;
        return true;
    }

    public String getApplicationPath() {
        return applicationPath;
    }}
