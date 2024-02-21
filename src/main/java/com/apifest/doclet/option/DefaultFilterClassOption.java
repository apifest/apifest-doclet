package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class DefaultFilterClassOption implements Option {
    private String defaultFilterClass;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the default filter class";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-defaultFilterClass", "--default-filter-class");
    }

    @Override
    public String getParameters() {
        return "class";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        defaultFilterClass = arguments.get(0);
        return true;
    }

    public String getDefaultFilterClass() {
        return defaultFilterClass;
    }
}
