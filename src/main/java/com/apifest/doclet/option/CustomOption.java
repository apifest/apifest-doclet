package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public abstract class CustomOption implements Option {
    private String value;

    public abstract String getOptionName();

    public abstract boolean isRequired();

    public boolean validate(String value) {
        return true;
    }

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "Sets the " + getOptionName();
    }

    @Override
    public Kind getKind() {
        return Doclet.Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of(getOptionName());
    }

    @Override
    public String getParameters() {
        return "value";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        value = arguments.get(0);
        if (isRequired() && (value == null || value.isEmpty())) {
            throw new IllegalArgumentException(getOptionName() + " is not set.");
        }
        if (!validate(value)) {
            throw new IllegalArgumentException("Invalid value provided for " + getOptionName());
        }
        return true;
    }

    public String getValue() {
        return value;
    }
}
