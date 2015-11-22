package com.outbrain.ob1k.swagger.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.server.netty.ResponseBuilder;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.QueryParameter;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.asList;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static java.util.Collections.unmodifiableList;

public class SwaggerService implements Service {

  private final ServiceRegistryView serviceRegistry;
  private final List<Class<? extends Service>> ignoredServices;

  public SwaggerService(final ServiceRegistryView serviceRegistry, final Class<? extends Service>... ignoredServices) {
    this.serviceRegistry = serviceRegistry;
    this.ignoredServices = asList(SwaggerService.class, ignoredServices);
  }

  public ComposableFuture<Response> apiDocs(final Request request) {
    return ComposableFutures.fromValue(buildJsonResponse(buildSwagger(request)));
  }

  public List<Class<? extends Service>> getIgnoredServices() {
    return unmodifiableList(ignoredServices);
  }

  private Swagger buildSwagger(final Request request) {
    final Swagger swagger = new Swagger();
    swagger.host(request.getHeader("Host"));
    swagger.info(buildInfo());
    for (final Map.Entry<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> entry :
            serviceRegistry.getRegisteredEndpoints().entrySet()) {
      final Path path = new Path();
      for (final Map.Entry<HttpRequestMethodType, AbstractServerEndpoint> endpointEntry : entry.getValue().entrySet()) {
        final HttpRequestMethodType methodType = endpointEntry.getKey();
        final String key = entry.getKey();
        final AbstractServerEndpoint endpoint = endpointEntry.getValue();
        if (!ignoreEndpoint(endpoint)) {
          final Tag tag = buildTag(endpoint.service.getClass());
          swagger.addTag(tag);
          switch (methodType) {
            case GET:
            case ANY:
              path.get(buildOperation(endpoint, tag, methodType));
              break;
            case POST:
              path.post(buildOperation(endpoint, tag, methodType));
              break;
            case PUT:
              path.put(buildOperation(endpoint, tag, methodType));
              break;
            case DELETE:
              path.delete(buildOperation(endpoint, tag, methodType));
              break;
            default:
              throw new UnsupportedOperationException("Unsupported method type " + methodType);
          }
          swagger.path(key, path);
        }
      }
    }
    return swagger;
  }

  private Tag buildTag(final Class<? extends Service> serviceClass) {
    final Api annotation = serviceClass.getAnnotation(Api.class);
    final String name = (annotation != null) ? annotation.value() : serviceClass.getSimpleName();
    final String description = (annotation != null) ? annotation.description() : serviceClass.getCanonicalName();
    return new Tag().name(name).description(description);
  }

  private String buildTitle() {
    final String contextPath = serviceRegistry.getContextPath();
    return contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
  }

  private Info buildInfo() {
    return new Info().description("API Documentation").version("1.0").title(buildTitle());
  }

  private Operation buildOperation(final AbstractServerEndpoint endpoint, final Tag tag, final HttpRequestMethodType methodType) {
    final Operation operation = new Operation().summary(endpoint.getTargetAsString()).tag(tag.getName()).
            operationId(endpoint.getTargetAsString() + "Using" + methodType.name());
    int i = 0;
    for (final Parameter parameter : endpoint.method.getParameters()) {
      final ApiParam annotation = parameter.getAnnotation(ApiParam.class);
      final String type = getSwaggerDataType(parameter);
      final String paramName = (annotation != null) ? annotation.name() : endpoint.paramNames[i++];
      final QueryParameter param = new QueryParameter().type(type).name(paramName);
      if (annotation != null) {
        param.description(annotation.value());
      }
      operation.addParameter(param);
    }
    return operation;
  }

  private String getSwaggerDataType(final Parameter parameter) {
    // TODO something better
    return "undefined";
  }

  private boolean ignoreEndpoint(final AbstractServerEndpoint endpoint) {
    final Class<?> serviceClass = endpoint.method.getDeclaringClass();
    return ignoredServices.contains(serviceClass);
  }

  private Response buildJsonResponse(final Object value) {
    try {
      final StringWriter buffer = new StringWriter();
      final ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      mapper.writeValue(buffer, value);
      return ResponseBuilder.ok()
              .withContent(buffer.toString())
              .addHeader(CONTENT_TYPE, "application/json; charset=UTF-8")
              .build();

    } catch (final IOException e) {
      return ResponseBuilder.fromStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR).
              withContent(e.getMessage()).build();
    }
  }

}
