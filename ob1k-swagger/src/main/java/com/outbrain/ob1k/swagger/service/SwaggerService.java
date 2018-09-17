package com.outbrain.ob1k.swagger.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.server.netty.ResponseBuilder;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.asList;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static java.util.Collections.unmodifiableList;

public class SwaggerService implements Service {

  private final ServiceRegistryView serviceRegistry;
  private final List<Class<? extends Service>> ignoredServices;

  @SafeVarargs
  public SwaggerService(final ServiceRegistryView serviceRegistry, final Class<? extends Service>... ignoredServices) {
    this.serviceRegistry = serviceRegistry;
    this.ignoredServices = asList(SwaggerService.class, ignoredServices);
  }

  public ComposableFuture<Response> apiDocs(final Request request) {
    return ComposableFutures.fromValue(buildJsonResponse(buildSwagger(request)));
  }

  List<Class<? extends Service>> getIgnoredServices() {
    return unmodifiableList(ignoredServices);
  }

  private Swagger buildSwagger(final Request request) {
    final Swagger swagger = new Swagger();
    swagger.host(request.getHeader("Host"));
    swagger.info(buildInfo());                          


    Set<ISwaggerAware> invoked = Sets.newHashSet();
    for (final Map.Entry<String, Map<HttpRequestMethodType, ServerEndpointView>> entry :
            serviceRegistry.getRegisteredEndpoints().entrySet()) {
      final Path path = new Path();
      for (final Map.Entry<HttpRequestMethodType, ServerEndpointView> endpointEntry : entry.getValue().entrySet()) {
        final HttpRequestMethodType methodType = endpointEntry.getKey();
        final String key = entry.getKey();
        final ServerEndpointView endpoint = endpointEntry.getValue();
        final Service service = endpoint.service();
        if (!ignoreEndpoint(endpoint)) {
          final Tag tag = buildTag(endpoint.getMethod().getDeclaringClass());
          swagger.addTag(tag);
          if (service instanceof ISwaggerAware) {
            ISwaggerAware swaggerAware = (ISwaggerAware) service;
            if(invoked.add(swaggerAware)) {
              swaggerAware.invoke(swagger, key);
            }
          } else {
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
    }
    return swagger;
  }

  private Tag buildTag(final Class<?> serviceClass) {
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

  private Operation buildOperation(final ServerEndpointView endpoint, final Tag tag, final HttpRequestMethodType methodType) {
    final Operation operation = new Operation().summary(endpoint.getTargetAsString()).tag(tag.getName()).
            operationId(endpoint.getTargetAsString() + "Using" + methodType.name());
    final String[] endpointParamNames = endpoint.getParamNames();

    ApiImplicitParams implicitParamAnnotation = findAnnotation(endpoint.getMethod().getAnnotations(), ApiImplicitParams.class);
    if (implicitParamAnnotation != null) {
      for (ApiImplicitParam param : implicitParamAnnotation.value()) {
        String dataType = param.dataType();
        String paramName = param.name();
        String paramType = param.paramType();
        if (paramType.equals("body")) {
          Class<?> type = null;
          try {
            type = Class.forName(dataType);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
          Map<String, Model> model = ModelConverters.getInstance().read(type);
          if (model.size() == 1) {
            BodyParameter bodyParameter = new BodyParameter().name(paramName).schema(model.values().iterator().next());
            bodyParameter.setRequired(true);
            operation.addParameter(bodyParameter);
          }

        } else if (paramType.equals("path")) {
          PathParameter pathParameter = new PathParameter().name(paramName).type(dataType);
          pathParameter.setRequired(true);
          operation.addParameter(pathParameter);
        } else if (paramType.equals("header")) {
          HeaderParameter pathParameter = new HeaderParameter().name(paramName).type(dataType);
          pathParameter.setRequired(param.required());
          operation.addParameter(pathParameter);
        } else if (paramType.equals("query")) {
          QueryParameter pathParameter = new QueryParameter().name(paramName).type(dataType);
          pathParameter.setRequired(param.required());
          operation.addParameter(pathParameter);
        }
      }
    }

    final Annotation[][] parameterAnnotations = endpoint.getMethod().getParameterAnnotations();
    final Class<?>[] parameterTypes = endpoint.getMethod().getParameterTypes();
    for (int i = 0; i < endpointParamNames.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      if (Request.class.equals(parameterType) && implicitParamAnnotation != null) {
        continue;
      }
      final ApiParam annotation = findAnnotation(parameterAnnotations[i], ApiParam.class);
      final String type = getSwaggerDataType(parameterType);
      final String paramName = (annotation != null) ? annotation.name() : endpointParamNames[i];
      final QueryParameter param = new QueryParameter().type(type).name(paramName);
      if (annotation != null) {
        param.description(annotation.value());
      }
      operation.addParameter(param);
    }
    return operation;
  }

  private <T> T findAnnotation(Annotation[] annotations, Class<T> t) {
    for (Annotation annotation : annotations) {
      if (t.equals(annotation.annotationType())) {
        return t.cast(annotation);
      }
    }
    return null;
  }

  private String getSwaggerDataType(final Class<?> parameterType) {
    // TODO suppport format and complex types
    return SwaggerDataType.forClass(parameterType);
  }

  private boolean ignoreEndpoint(final ServerEndpointView endpoint) {
    final Class<?> serviceClass = endpoint.getMethod().getDeclaringClass();
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
