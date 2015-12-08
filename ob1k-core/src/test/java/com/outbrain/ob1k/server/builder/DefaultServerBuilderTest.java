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

// JDK 8 version
//    Server server = DefaultServerBuilder.newBuilder().contextPath("contextPath").
//            resource(r -> r.staticMapping("virtualPath", "realPath").staticPath("path")).
//            service(s -> s.register(new TestService(), "/path",
//                      b -> b.endpoint("testMethod", "/test").endpoint("anotherMethod", "/another"), new TestServiceFilter()).
//                    register(new TestService2(), "/path2")).
//            configure(c -> c.usePort(8080).acceptKeepAlive(true).supportZip(true).requestTimeout(100, TimeUnit.MILLISECONDS)).build();



    Server server = DefaultServerBuilder.newBuilder().contextPath("contextPath").
            resource(new BuilderProvider<DefaultResourceMappingBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final DefaultResourceMappingBuilder builder) {
                builder.staticMapping("virtualPath", "realPath").staticPath("path");
              }
            }).
            service(new BuilderProvider<DefaultServiceRegisterBuilder>() {
              @Override
              public void provide(final DefaultServiceRegisterBuilder builder) {
                // should be lambda to whoever is inJDK 8
                builder.register(new TestService(), "/path", new BuilderProvider<DefaultServiceBindBuilder>() {
                  @Override
                  public void provide(final DefaultServiceBindBuilder builder) {
                    builder.endpoint("testMethod", "/test").endpoint("anotherMethod", "/another");
                  }
                }, new TestServiceFilter()).register(new TestService2(), "/path2");
              }
            }).
            configure(new BuilderProvider<DefaultConfigureBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final DefaultConfigureBuilder builder) {
                builder.usePort(8080).acceptKeepAlive(true).supportZip(true).requestTimeout(100, TimeUnit.MILLISECONDS);
              }
            }).build();




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