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
        return List.of("applicationPath");
    }

    @Override
    public String getParameters() {
        return "path";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        applicationPath = arguments.get(0);
        return true;
    }

    public String getApplicationPath() {
        return applicationPath;
    }}
