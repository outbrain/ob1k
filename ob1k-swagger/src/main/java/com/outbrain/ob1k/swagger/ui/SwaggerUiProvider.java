package com.outbrain.ob1k.swagger.ui;

import com.outbrain.ob1k.server.build.StaticResourcesPhase;
import com.outbrain.ob1k.server.build.StaticResourcesProvider;

public class SwaggerUiProvider implements StaticResourcesProvider {

  public static final String SWAGGER_UI_URL = "/api/swagger-ui.html";

  @Override
  public void configureResources(final StaticResourcesPhase builder) {
    builder.addStaticPath("/html");
    builder.addStaticPath("/css");
    builder.addStaticResource("/api/webjars", "/META-INF/resources/webjars");
    builder.addStaticResource("/api/images", "META-INF/resources/webjars/springfox-swagger-ui/images");
    builder.addStaticMapping(SWAGGER_UI_URL, "/META-INF/resources/swagger-ui.html");
    builder.addStaticResource("/api/configuration", "/swagger-ui/configuration");
    builder.addStaticResource("/api/swagger-resources", "/swagger-ui/swagger-resources");
  }
}
