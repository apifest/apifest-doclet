package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class BackendHostOption implements Option {
    private String backendHost;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the backend host";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("backendHost");
    }

    @Override
    public String getParameters() {
        return "host";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        backendHost = arguments.get(0);
        return true;
    }

    public String getBackendHost() {
        return backendHost;
    }
}
