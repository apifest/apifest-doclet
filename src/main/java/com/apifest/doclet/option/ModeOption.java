package com.apifest.doclet.option;

import com.apifest.doclet.DocletMode;
import jdk.javadoc.doclet.Doclet.Option;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModeOption implements Option {
    private Set<DocletMode> docletModes = new HashSet<>();

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the mode";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        String s = arguments.get(0);
        if (s == null || s.isEmpty()) {
            docletModes.add(DocletMode.MAPPING); // default to mapping
        } else {
            String[] modesSplit = s.split(",");
            for (String modeSplit : modesSplit) {
                DocletMode mode = DocletMode.fromString(modeSplit);
                if (mode == null) {
                    throw new IllegalArgumentException("One of the modes you have specified is invalid: " + modeSplit);
                }
                docletModes.add(mode);
            }
        }
        return true;
    }

    @Override
    public List<String> getNames() {
        return List.of("mode");
    }

    @Override
    public String getParameters() {
        return "mode";
    }

    public Set<DocletMode> getDocletModes() {
        return docletModes;
    }
}
