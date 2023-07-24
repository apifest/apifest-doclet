package com.apifest.doclet.option;

import jdk.javadoc.doclet.Doclet.Option;

import java.util.List;

public class BackendPortOption implements Option {
    private int backendPort;

    @Override
    public int getArgumentCount() {
        return 1;  // The option requires one argument
    }

    @Override
    public String getDescription() {
        return "Sets the backend port";
    }

    @Override
    public Kind getKind() {
        return Option.Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("backendPort");
    }

    @Override
    public String getParameters() {
        return "port";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        String backendPort = arguments.get(0);
        if (backendPort == null || backendPort.isEmpty() || "null".equalsIgnoreCase(backendPort)) {
            System.out.println("ERROR: backend.port is not set");
            throw new IllegalArgumentException("backend.host is not set.");
        }
        try {
            this.backendPort = Integer.parseInt(backendPort);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("backendPort is not an integer.");
        }
        return true;
    }

    public int getBackendPort() {
        return backendPort;
    }

}
