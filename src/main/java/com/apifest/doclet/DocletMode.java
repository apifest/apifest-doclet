package com.apifest.doclet;

public enum DocletMode {
    MAPPING("mapping"), DOC("doc");
    private String mode;

    private DocletMode(String mode) {
        this.mode = mode;
    }

    public String getValue() {
        return this.mode;
    }

    public static DocletMode fromString(String value) {
        DocletMode[] modes = DocletMode.values();
        for (DocletMode mode : modes) {
            if (mode.getValue().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
