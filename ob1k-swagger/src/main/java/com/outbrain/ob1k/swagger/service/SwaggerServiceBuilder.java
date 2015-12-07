package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.BuilderSection;
import com.outbrain.ob1k.server.builder.DefaultResourceMappingBuilder;
import com.outbrain.ob1k.server.builder.DefaultServiceRegisterBuilder;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

import java.util.List;

public class SwaggerServiceBuilder<E extends ExtendableServerBuilder<E>> extends BuilderSection<E> {

  public static final String SWAGGER_UI_URI = "api/swagger-ui.html";

  private final DefaultServiceRegisterBuilder<E> serviceBuilder;
  private final DefaultResourceMappingBuilder<E> resourceBuilder;
  private final ServerBuilderState state;


  public SwaggerServiceBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
    this.serviceBuilder = new DefaultServiceRegisterBuilder<>(builder, state);
    this.resourceBuilder = new DefaultResourceMappingBuilder<>(builder, state);
  }

  @SuppressWarnings("unchecked")
  public E registerSwaggerService(final String path, final List<Class<? extends Service>> ignoredServices, final ServiceFilter... filters) {
    final Class[] ignoredServicesArray = ignoredServices.toArray(new Class[ignoredServices.size()]);
    serviceBuilder.register(new SwaggerService(state.getRegistry(), ignoredServicesArray), path, filters);
    resourceBuilder.staticPath("/html").
    staticPath("/css").
    staticResource("/api/webjars", "/META-INF/resources/webjars").
    staticResource("/api/images", "META-INF/resources/webjars/springfox-swagger-ui/images").
    staticMapping("/" + SWAGGER_UI_URI, "/META-INF/resources/swagger-ui.html").
    staticResource("/api/configuration", "/swagger-ui/configuration").
    staticResource("/api/swagger-resources", "/swagger-ui/swagger-resources");

    return backToServerBuilder();
  }
}
