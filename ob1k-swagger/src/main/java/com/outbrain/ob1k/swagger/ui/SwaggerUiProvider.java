package com.outbrain.ob1k.swagger.ui;

import com.outbrain.ob1k.server.build.StaticResourcesPhase;
import com.outbrain.ob1k.server.build.StaticResourcesProvider;

public class SwaggerUiProvider implements StaticResourcesProvider {

  public static final String SWAGGER_UI_URL = "/swagger-ui.html";

  @Override
  public void configureResources(final StaticResourcesPhase builder) {
    builder.addStaticPath("/html");
    builder.addStaticPath("/css");
    builder.addStaticResource("/webjars", "/META-INF/resources/webjars");
    builder.addStaticResource("/images", "META-INF/resources/webjars/springfox-swagger-ui/images");
    builder.addStaticResource(SWAGGER_UI_URL, "/META-INF/resources/swagger-ui.html");
    builder.addStaticResource("/configuration", "/swagger-ui/configuration");
    builder.addStaticResource("/swagger-resources", "/swagger-ui/swagger-resources");
  }
}
