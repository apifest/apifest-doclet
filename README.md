#ApiFest Doclet
ApiFest Doclet is a tool that generates ApiFest mapping configuration file (XML) from Javadoc.
Here are the custom Javadoc annotations that ApiFest Doclet is aware of:

- @apifest.external - the endpoint visible to the world;
- @apifest.internal - your API endpoint;
- @apifest.actions - the class name(s) of the action(s) that will be executed before requests hit your API;
- @apifest.filters - the class name(s) of the filter(s) that will be executed before responses from API are returned back;
- @apifest.scope - scope(s)(comma-separated list) of the endpoint;
- @apifest.token.user - *true* if user authentication is required, otherwise *false*.

Currently, JAX-RS HTTP method annotations are used for setting the HTTP method of the endpoint.



