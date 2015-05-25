package com.apifest.doclet;

import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;

public class ParsedEndpoint {
    private MappingEndpoint mappingEndpoint;
    private MappingEndpointDocumentation mappingEndpointDocumentation;
    public MappingEndpoint getMappingEndpoint() {
        return mappingEndpoint;
    }
    public void setMappingEndpoint(MappingEndpoint mappingEndpoint) {
        this.mappingEndpoint = mappingEndpoint;
    }
    public MappingEndpointDocumentation getMappingEndpointDocumentation() {
        return mappingEndpointDocumentation;
    }
    public void setMappingEndpointDocumentation(MappingEndpointDocumentation mappingEndpointDocumentation) {
        this.mappingEndpointDocumentation = mappingEndpointDocumentation;
    }
}
