package com.outbrain.ob1k.swagger.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import com.outbrain.ob1k.server.registry.endpoints.AsyncServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.QueryParameter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.outbrain.ob1k.HttpRequestMethodType.ANY;
import static com.outbrain.ob1k.HttpRequestMethodType.GET;
import static com.outbrain.ob1k.HttpRequestMethodType.POST;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SwaggerServiceTest {

  public static final String CONTEXT_PATH = "/Ob1kService";
  public static final String HOST = "myhost:8080";
  public static final int OK = 200;

  @Mock
  private ServiceRegistry registry;
  @Mock
  private Request request;

  private SortedMap<String, Map<HttpRequestMethodType, ServerEndpointView>> endpointsByPathMap;

  private SwaggerService service;

  @Before
  public void setup() {
    endpointsByPathMap = new TreeMap<>();

    when(registry.getContextPath()).thenReturn(CONTEXT_PATH);
    when(registry.getRegisteredEndpoints()).thenReturn(endpointsByPathMap);
  }

  @Test
  public void shouldReturnEndpointsThatAreUnderPath() throws Exception {
    service = new SwaggerService(registry);
    final String expected = createData();

    // when
    final Response response = service.apiDocs(request).get();
    //then
    Assert.assertEquals(OK, response.getStatus());
    Assert.assertEquals(expected, response.getRawContent());
  }

  @Test
  public void shouldIgnoreEndpointsFilteredByService() throws Exception {
    service = new SwaggerService(registry, IgnoredService.class);
    final String expected = createData();
    createData(IgnoredService.class, "/api", null, endpointsByPathMap, GET);

    // when
    final Response response = service.apiDocs(request).get();
    //then
    Assert.assertEquals(OK, response.getStatus());
    Assert.assertEquals(expected, response.getRawContent());
  }

  private String createData() throws Exception {
    final Swagger expected = new Swagger();

    expected.host(HOST);
    when(request.getHeader("Host")).thenReturn(HOST);

    expected.info(new Info().title(CONTEXT_PATH.substring(1)).description("API Documentation").version("1.0"));

    createData(DummyService.class, "/api", expected, endpointsByPathMap, GET);
    createData(DummyService.class, "/api", expected, endpointsByPathMap, POST);
    createData(AnnotatedDummyService.class, "/apiAnnotated", expected, endpointsByPathMap,
            "an annotated test service", ANY, "millis", "millis since epoch");

    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsString(expected);
  }

  private void createData(final Class<? extends Service> serviceClass,
                          final String servicePath,
                          final Swagger expected,
                          final Map<String, Map<HttpRequestMethodType, ServerEndpointView>> endpointsByPathMap,
                          final HttpRequestMethodType methodType) throws Exception {
    createData(serviceClass, servicePath, expected, endpointsByPathMap,
            serviceClass.getCanonicalName(), methodType);
  }

    private void createData(final Class<? extends Service> serviceClass,
                            final String servicePath,
                            final Swagger expected,
                            final Map<String, Map<HttpRequestMethodType, ServerEndpointView>> endpointsByPathMap,
                            final String description, final HttpRequestMethodType methodType, final String... additional) throws Exception {
    for (final Method method : serviceClass.getDeclaredMethods()) {
      final String pathKey = CONTEXT_PATH + servicePath + "/" + method.getName();
      int i=0;
      final String[] parameterNames = new String[method.getParameterCount()];
      for (final Parameter parameter : method.getParameters()) {
        parameterNames[i++] = parameter.getName();
      }
      if (expected != null) {
        expected.tag(new Tag().name(serviceClass.getSimpleName()).description(description));
        final String methodName = method.getDeclaringClass().getCanonicalName() + "." + method.getName()
                + "(" + Joiner.on(",").join(parameterNames) + ")";
        final Operation operation = new Operation().tag(serviceClass.getSimpleName()).summary(methodName).
                operationId(methodName + "Using"+methodType);
        Path path = expected.getPath(pathKey);
        if (path == null) {
          path = new Path();
        }
        switch (methodType) {
          case ANY:
          case GET:
            path = path.get(operation);
            break;
          case POST:
            path  = path.post(operation);
            break;
        }
        expected.path(pathKey, path);
        i=0;
        for (final Parameter parameter : method.getParameters()) {
          final String name = (additional.length > i) ? additional[i++] : parameter.getName();
          final QueryParameter param = new QueryParameter().name(name).
                  type("undefined");
          if (additional.length > i) {
            param.description(additional[i++]);
          }
          operation.parameter(param);
        }
      }
      endpointsByPathMap.putIfAbsent(pathKey, new HashMap<HttpRequestMethodType, ServerEndpointView>());
      final Map<HttpRequestMethodType, ServerEndpointView> endpointMap = endpointsByPathMap.get(pathKey);
      endpointMap.put(methodType,
              new AsyncServerEndpoint(serviceClass.newInstance(),
                      new AsyncFilter[0],
                      method,
                      methodType,
                      parameterNames));
    }
  }

}