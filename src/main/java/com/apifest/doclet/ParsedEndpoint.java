/*
* Copyright 2013-2015, ApiFest project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.apifest.doclet;

import com.apifest.api.MappingEndpoint;
import com.apifest.api.MappingEndpointDocumentation;

/**
 * Holds all the parsed data for an endpoint.
 * @author Ivan Georgiev
 *
 */
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
