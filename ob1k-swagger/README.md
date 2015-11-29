# ob1k-swagger

Ob1k swagger module provides Swagger support to Ob1k servers.

The SwaggerService has a single endpoint that will return a Swagger2 complaient protocol which will describe all endpoints defined in this server.

SwaggerService needs access to the internal registry and is constructed during the build phase by an instance of the SwaggerServiceProvider as shown in the example RestServer.

> builder.addServices(new SwaggerServiceProvider(), "/api/swagger");

In order to also integrate the Swagger UI we need to map the swagger-ui resources (js, html, css files) to ob1k. (Also shown in RestServer)

> builder.configureStaticResources(new SwaggerUiProvider());

Currently SwaggerService supports the basic publishing of the server's endpoints.
It also supports the @Api and @ApiParam swagger annotations.

TODO - 
* Map param types to swagger protocol datatypes.
* Support more swagger annotations.



