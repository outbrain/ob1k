package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ServerBuilderTest {


  @Test
  public void shouldBuildServer() {
    final Server server = ServerBuilder.newBuilder().
            contextPath("contextPath").
            resource(builder ->
                builder.staticMapping("virtualPath", "realPath").staticPath("path")).
            service(builder ->
                builder.register(new TestService(), "/path", bind ->
                    bind.endpoint("testMethod", "/test").endpoint("anotherMethod", "/another"))
                .withFilters(new TestServiceFilter())
                .register(new TestService2(), "/path2")).
            configure(builder ->
                builder.usePort(8080).acceptKeepAlive(true).supportZip(true).requestTimeout(100, TimeUnit.MILLISECONDS)).
            build();
    Assert.assertEquals("contextPath", server.getContextPath());
  }


  private class TestService implements Service {

    public ComposableFuture<String> testMethod() { return null; }

    public ComposableFuture<String> anotherMethod() { return null; }
  }

  private class TestServiceFilter implements ServiceFilter {
  }

  private class TestService2 implements Service {
  }
}