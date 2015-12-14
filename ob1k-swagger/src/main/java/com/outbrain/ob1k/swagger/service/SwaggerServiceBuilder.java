package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.ExtensionBuilder;
import com.outbrain.ob1k.server.builder.ResourceMappingBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder;

import java.util.List;

public class SwaggerServiceBuilder implements ExtensionBuilder {

  public static final String SWAGGER_UI_URI = "api/swagger-ui.html";

  private final String path;
  private final List<Class<? extends Service>> ignoredServices;
  private final ServiceFilter[] filters;


  public static SwaggerServiceBuilder enableSwagger(final String path, final List<Class<? extends Service>> ignoredServices, final ServiceFilter... filters) {
    return new SwaggerServiceBuilder(path, ignoredServices, filters);
  }

  private SwaggerServiceBuilder(final String path, final List<Class<? extends Service>> ignoredServices, final ServiceFilter... filters) {
    this.path = path;
    this.ignoredServices = ignoredServices;
    this.filters = filters;
  }

  @Override
  @SuppressWarnings("unchecked") // small price to pay for Class<Service>[] varargs
  public void apply(final ServerBuilderState state) {
    final ServiceRegisterBuilder serviceBuilder = new ServiceRegisterBuilder(state);
    final ResourceMappingBuilder resourceBuilder = new ResourceMappingBuilder(state);
    final Class[] ignoredServicesArray = ignoredServices.toArray(new Class[ignoredServices.size()]);
    serviceBuilder.register(new SwaggerService(state.getRegistry(), ignoredServicesArray), path, filters);
    resourceBuilder.staticPath("/html").
    staticPath("/css").
    staticResource("/api/webjars", "/META-INF/resources/webjars").
    staticResource("/api/images", "META-INF/resources/webjars/springfox-swagger-ui/images").
    staticMapping("/" + SWAGGER_UI_URI, "/META-INF/resources/swagger-ui.html").
    staticResource("/api/configuration", "/swagger-ui/configuration").
    staticResource("/api/swagger-resources", "/swagger-ui/swagger-resources");
  }
}
