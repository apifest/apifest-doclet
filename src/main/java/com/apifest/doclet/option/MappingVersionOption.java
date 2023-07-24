package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class MappingVersionOption implements Option {
    private String mappingVersion;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the mapping version";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("mappingVersion");
    }

    @Override
    public String getParameters() {
        return "version";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        mappingVersion = arguments.get(0);
        return true;
    }

    public String getMappingVersion() {
        return mappingVersion;
    }
}
