package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.*;
import java.util.stream.Collectors;

public class CustomAnnotationOption implements Option {
    private Map<String, List<String>> customAnnotations = new HashMap<>();

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the custom annotations";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-customAnnotations", "--custom-annotations");
    }

    @Override
    public String getParameters() {
        return "annotations";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        String customAnnotationsValue = arguments.get(0);
        for (String annotation : customAnnotationsValue.split(",")) {
            if (annotation.contains(":")) {
                String[] tokens = annotation.split(":");
                List<String> annotationAttributeList = customAnnotations.computeIfAbsent(tokens[0], k -> new ArrayList<>());
                annotationAttributeList.add(tokens[1]);
            } else {
                customAnnotations.put(annotation, Collections.emptyList());
            }
        }
        return true;
    }

    public Map<String, List<String>> getCustomAnnotations() {
        return customAnnotations;
    }
}
