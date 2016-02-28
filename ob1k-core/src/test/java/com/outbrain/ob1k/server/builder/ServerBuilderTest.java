package com.outbrain.ob1k.server.builder;

import com.google.common.base.Joiner;
import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.HttpRequestMethodType.ANY;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

public class ServerBuilderTest {


  @Test
  public void shouldBuildServer() {
    final RegistryHolder registryHolder = new RegistryHolder();

    final Server server = ServerBuilder.newBuilder().
            contextPath("contextPath").
            resource(builder ->
                builder.staticMapping("virtualPath", "realPath").staticPath("path")).
            service(builder ->
                builder.register(new TestService(), "/path", bind ->
                        bind.endpoint("testMethod", "/test").
                                endpoint("anotherMethod", "/another", new AnotherTestServiceFilter()))
                        .withFilters(new TestServiceFilter())
                .register(new TestService(), "/path2")).
            configure(builder ->
                builder.usePort(8080).acceptKeepAlive(true).supportZip(true).requestTimeout(100, TimeUnit.MILLISECONDS)).
            withExtension(registryHolder). // for grabbing the registry to use later by asserts
            build();


    assertEquals("contextPath", server.getContextPath());
    assertEquals(4, registryHolder.getRegistry().getRegisteredEndpoints().size());

    assertEndpointValues(registryHolder.getRegistry().getRegisteredEndpoints().get("/contextPath/path/test").get(ANY), "testMethod", "testParam", ":TestServiceFilter");
    assertEndpointValues(registryHolder.getRegistry().getRegisteredEndpoints().get("/contextPath/path/another").get(ANY), "anotherMethod", "", ":AnotherTestServiceFilter:TestServiceFilter");
    assertEndpointValues(registryHolder.getRegistry().getRegisteredEndpoints().get("/contextPath/path2/testMethod").get(ANY), "testMethod", "testParam", "");
    assertEndpointValues(registryHolder.getRegistry().getRegisteredEndpoints().get("/contextPath/path2/anotherMethod").get(ANY), "anotherMethod", "", "");
  }

  private void assertEndpointValues(final ServerEndpointView endpoint, final String method, final String paramNames, final String filters) {
    assertEquals(method, endpoint.getMethod().getName());
    assertEquals(paramNames, Joiner.on(" ").join(asList(endpoint.getParamNames())));
    assertEquals(filters, stream(endpoint.getFilters()).map(it -> it.getClass().getSimpleName()).reduce("", (acc, it) -> acc + ":" + it));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failOnBindNonExistingMethod() {
    final RegistryHolder registryHolder = new RegistryHolder();

    final Server server = ServerBuilder.newBuilder().
      contextPath("contextPath").
      service(builder ->
        builder.register(new TestService(), "/path",
          bind -> bind.endpoint("testMethodNotExist", "/test"))).
      withExtension(registryHolder). // for grabbing the registry to use later by asserts
      build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void failOnBindSyncMethod() {
    final RegistryHolder registryHolder = new RegistryHolder();

    final Server server = ServerBuilder.newBuilder().
      contextPath("contextPath").
      service(builder ->
        builder.register(new TestService(), "/path",
          bind -> bind.endpoint("syncMethod", "/sync"))).
      withExtension(registryHolder). // for grabbing the registry to use later by asserts
      build();
  }

  private class TestService implements Service {

    public ComposableFuture<String> testMethod(final String testParam) {
      return null;
    }

    public ComposableFuture<String> anotherMethod() { return null; }

    public String syncMethod() { return null; }
  }

  private class TestServiceFilter implements AsyncFilter {
    @Override
    public ComposableFuture handleAsync(final AsyncRequestContext ctx) {
      return null;
    }
  }

  private class AnotherTestServiceFilter implements AsyncFilter {
    @Override
    public ComposableFuture handleAsync(final AsyncRequestContext ctx) {
      return null;
    }
  }

  private class RegistryHolder implements ExtensionBuilder {

    private ServiceRegistryView registry;

    @Override
    public void apply(final ServerBuilderState builder) {
      registry = builder.getRegistry();
    }

    public ServiceRegistryView getRegistry() {
      return registry;
    }
  }
}