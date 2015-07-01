#ApiFest Doclet

ApiFest Doclet is a tool that generates ApiFest mapping configuration file (XML) from Javadoc. ApiFest Doclet supports two modes: "mapping" and "doc". When using
the first mode the doclet is generating a XML file containing the mapping endpoints. In the second mode the Doclet generates a json file that contains documentation for every endpoint.
The tags supported by the doclet are outlined in the following sections.

###Endpoint level tags

These tags describe the general properties of the endpoint:

- @apifest.external - the endpoint visible to the world.
- @apifest.internal - your API endpoint.
- @apifest.hidden - when added, that endpoint will not be included in the mappings.
- @apifest.action - the class name of the action that will be executed before requests hit your API.
- @apifest.filter - the class name of the filter that will be executed before responses from API are returned back.
- @apifest.scope - scope(s)(space-separated list) of the endpoint.
- @apifest.auth.type - *user* if user authentication is required, *client-app* if only client application authentication is required. 
Note, that if the endpoint could be accessible without access token, then just skip this tag.
- @apifest.re.{varName} - regular expression used for variable with name {varName} (without brackets); if several variables, then
add @apifest.re.{varName} for each of them.
- @apifest.docs.description - long text description for the endpoint.
- @apifest.docs.summary - short text description for the endpoint.
- @apifest.docs.group - can be used to group endpoints. 
- @apifest.docs.hidden - when added, that endpoint won't be included in the mapping documentation.
- @apifest.docs.exampleRequest - can be used to document an example request for this endpoint
- @apifest.docs.exampleResult - can be used to document an example response from this endpoint

Currently, JAX-RS HTTP method annotations are used for setting the HTTP method of the endpoint.

###Request Parameter level tags

Typically, when a user calls an endpoint he/she must provide a set of request parameters. The following tags can be used to document these parameters:

- @apifest.docs.paramsDescription - a general description of the request.
- @apifest.docs.params.{parameterName} - specifying this tag will define a request parameter with name {parameterName} and description the value of the tag.    
For example,    
    @apifest.docs.params.myParameter This is my parameter!    
will result in a parameter with name myParameter and description 'This is my parameter!'.    
Note that the {parameterName} cannot contain the "." delimiter.
- @apifest.docs.params.{parameterName}.name allows overriding the name. You might need this if you want to have a parameter name including special characters like the '.' delimiter or any character that breaks the Javadoc tag(whitespace, brackets,etc.)
- @apifest.docs.params.{parameterName}.type - documents the type of the parameter. As far as the documentation is concerned, this is just a simple string and it does not make assumptions or guarantees about the actual type of the parameter.
- @apifest.docs.params.{parameterName}.exampleValue - can be used to document an example value for this request parameter.
- @apifest.docs.params.{parameterName}.optional - the presence of this tag indicates that the parameter is optional. All parameters are required by default.

###Result Parameter level tags

Calling an endpoint should yield some kind of result which consists of a set of result parameters. The following tags can be used to document these result parameters:

- @apifest.docs.resultsDescription - a general description of the result.
- @apifest.docs.results.{parameterName} - specifying this tag will define a result parameter with name {parameterName} and description the value of the tag.    
For example,    
    @apifest.docs.results.myParameter This is my result!    
will result in a parameter with name myParameter and description 'This is my result!'.    
Note that the {parameterName} cannot contain the "." delimiter.
- @apifest.docs.results.{parameterName}.name allows overriding the name. You might need this if you want to have a parameter name including special characters like the '.' delimiter or any character that breaks the Javadoc tag(whitespace, brackets,etc.)
- @apifest.docs.results.{parameterName}.type - documents the type of the parameter(string, list, number, etc.). As far as the documentation is concerned, this is just a simple string and it does not make assumptions or guarantees about the actual type of the parameter.
- @apifest.docs.results.{parameterName}.optional - the presence of this tag indicates that the parameter is optional. All parameters are required by default.

###Endpoint exceptions tags

Sometimes users call the API in an incorrect manner and the API needs to respond with an error message. The following tags can be used to document these error messages:

- @apifest.docs.exceptions.{parameterName} - use this tag to define the name of the exception and the conditions in which this exception occurs.
- @apifest.docs.exceptions.{parameterName}.description - full description of the exception
- @apifest.docs.exceptions.{parameterName}.code - the code of the exception

##Features

- generates ApiFest mapping configuration file from your Javadoc - custom annotations used;
- keeps your code clean - no versions required in Javadoc annotations;
- all version/environment specific settings are passed as variables; 
- easy integration in maven projects


###Usage
ApiFest Doclet requires the following environment variables:

For both modes:

- mode - the Doclet support two modes in the moment ("mapping" and "doc").They can be comma separated.
- mapping.version - the version your API will be exposed externally;
- application.path - the application path used to obtain all application resources, it will be preprended to each internal path;

Only for the Doclet "mapping" mode:

- mapping.filename - the name of the mapping configuration file that will be generated;
- backend.host - the host(your API is running on) where requests should be translated to;
- backend.port - the port of the backend.host;
- defaultActionClass - the fully qualified action class that will be added if no action is declared in Javadoc annotations;
- defaultFilterClass - the fully qualified filter class that will be added if no filter is declared in Javadoc annotations.

Only for the Doclet "doc" mode:

- mapping.docs.filename - the name of the mapping documentation file that will be generated.

If your project uses maven, here is an example integration of ApiFest Doclet in "mapping" mode in your pom.xml:
```
...
<profiles>
    <profile>
      <id>gen-mapping</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>properties-maven-plugin</artifactId>
            <version>1.0-alpha-2</version>
            <executions>
              <execution>
                <phase>validate</phase>
                <goals>
                  <goal>read-project-properties</goal>
                </goals>
                <configuration>
                  <files>
                    <file>${basedir}/src/main/resources/project.properties</file>
                  </files>
                </configuration>
              </execution>
            </executions>
          </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>2.9.1</version>
            <executions>
              <execution>
                <phase>validate</phase>
                <goals>
                  <goal>javadoc</goal>
                </goals>
                <configuration>
                  <doclet>com.apifest.doclet.Doclet</doclet>
                  <docletArtifact>
                    <groupId>com.apifest</groupId>
                    <artifactId>apifest-doclet</artifactId>
                    <version>0.1.0</version>
                  </docletArtifact>
                  <additionalJOptions>
                    <additionalJOption>-J-Dmapping.version=${mapping.version}</additionalJOption>
                    <additionalJOption>-J-Dmapping.filename=${mapping.filename}</additionalJOption>
                    <additionalJOption>-J-Dmapping.docs.filename=${mapping.docs.filename}</additionalJOption>
                    <additionalJOption>-J-Dbackend.host=${backend.host}</additionalJOption>
                    <additionalJOption>-J-Dbackend.port=${backend.port}</additionalJOption>
                    <additionalJOption>-J-Dapplication.path=${application.path}</additionalJOption>
                    <additionalJOption>-J-DdefaultActionClass=${defaultActionClass}</additionalJOption>
                    <additionalJOption>-J-DdefaultFilterClass=${defaultFilterClass}</additionalJOption>
                    <additionalJOption>-J-Dmode=${mode}</additionalJOption>
                  </additionalJOptions>
                  <useStandardDocletOptions>false</useStandardDocletOptions>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
...
```

where *backend.host*, *backend.port* and *mode* are defined in *project.properties* file; *mapping.version*, *mapping.filename* and *mapping.docs.filename* 
could be defined in your pom.xml file so they stick to the code. *application.path* is the application path used to 
obtain all application resources.
Then you can use the following command in order to generate mapping xml file:
```mvn install -P gen-mapping```

Note, that *mapping.version* value will be prepended to the apifest.external annotation value.
If no value is set for *mapping.filename*, then *output_mapping_[mapping.version].xml* will be used. 
If the example pom integration is used, then the mapping file will be stored in *target/site/apidocs* directory. 