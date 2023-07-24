package com.apifest.doclet.option;
;
import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class MappingFilenameOption implements Option {
    private String mappingFilename;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the mapping filename";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("mappingFilename");
    }

    @Override
    public String getParameters() {
        return "filename";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        mappingFilename = arguments.get(0);
        return true;
    }

    public String getMappingFilename() {
        return mappingFilename;
    }
}
