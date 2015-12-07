package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class DefaultServerBuilderTest {


  @Test
  public void shouldBuildServer() {

    Server server = DefaultServerBuilder.newBuilder().contextPath("contextPath").
            configure().usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS).and().
            resource().staticMapping("virtualPath", "realPath").staticPath("path").and().
            service().register(new TestService(), "/path", new TestServiceFilter()).
              endpoint("testMethod", "/test").endpoint("anotherMethod", "/another").
            service().register(new TestService2(), "/path2").build();


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