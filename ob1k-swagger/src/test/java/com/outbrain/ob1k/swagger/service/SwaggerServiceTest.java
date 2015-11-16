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
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.AsyncServerEndpoint;
import com.outbrain.service.AnnotatedDummyService;
import com.outbrain.service.DummyService;
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
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.outbrain.ob1k.HttpRequestMethodType.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SwaggerServiceTest {

  public static final String CONTEXT_PATH = "/Ob1kService";
  public static final String HOST = "myhost:8080";

  @Mock
  private ServiceRegistry registry;
  @Mock
  private Request request;

  private SwaggerService service;

  @Before
  public void setup() {
    service = new SwaggerService(registry);
    when(registry.getContextPath()).thenReturn(CONTEXT_PATH);
  }

  @Test
  public void shouldReturnEndpointsThatAreUnderPath() throws Exception {
    String expected = createData();
    // when
    final Response response = service.apiDocs(request).get();
    //then
    Assert.assertEquals(OK, response.getStatus());
    Assert.assertEquals(expected, response.getRawContent());
  }


  private String createData() throws Exception {
    SortedMap<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> endpointsByPathMap = new TreeMap<>();
    Swagger expected = new Swagger();

    expected.host(HOST);
    when(request.getHeader("Host")).thenReturn(HOST);

    expected.info(new Info().title(CONTEXT_PATH.substring(1)).description("API Documentation").version("1.0"));

    createData(DummyService.class, "/api", expected, endpointsByPathMap);
    createData(Ob1kDummyService.class, "/NOTapi", null, endpointsByPathMap);
    createData(AnnotatedDummyService.class, "/apiAnnotated", expected, endpointsByPathMap,
            "an annotated test service", "millis", "millis since epoch");

    when(registry.getRegisteredEndpoints()).thenReturn(endpointsByPathMap);

    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsString(expected);
  }

  private void createData(final Class<? extends Service> serviceClass,
                          final String servicePath,
                          final Swagger expected,
                          final Map<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> endpointsByPathMap) throws Exception {
    createData(serviceClass, servicePath, expected, endpointsByPathMap,
            serviceClass.getCanonicalName());
  }

    private void createData(final Class<? extends Service> serviceClass,
                          final String servicePath,
                          final Swagger expected,
                          final Map<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> endpointsByPathMap,
                          final String description, final String... additional) throws Exception {
    for (Method method : serviceClass.getDeclaredMethods()) {
      final String path = CONTEXT_PATH + servicePath + "/" + method.getName();
      int i=0;
      String[] parameterNames = new String[method.getParameterCount()];
      for (Parameter parameter : method.getParameters()) {
        parameterNames[i++] = parameter.getName();
      }
      if (expected != null) {
        expected.tag(new Tag().name(serviceClass.getSimpleName()).description(description));
        String methodName = method.getDeclaringClass().getCanonicalName() + "." + method.getName()
                + "(" + Joiner.on(",").join(parameterNames) + ")";
        final Operation operation = new Operation().tag(serviceClass.getSimpleName()).summary(methodName).
                operationId(methodName + "UsingGET");
        expected.path(path, new Path().get(operation));
        i=0;
        for (Parameter parameter : method.getParameters()) {
          final String name = (additional.length > i) ? additional[i++] : parameter.getName();
          final QueryParameter param = new QueryParameter().name(name).
                  type("undefined");
          if (additional.length > i) {
            param.description(additional[i++]);
          }
          operation.parameter(param);
        }
      }
      endpointsByPathMap.put(path, Collections.<HttpRequestMethodType, AbstractServerEndpoint>singletonMap(
              GET,
              new AsyncServerEndpoint(serviceClass.newInstance(),
                      new AsyncFilter[0],
                      method,
                      GET,
                      parameterNames)));
    }
  }

}