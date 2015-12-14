package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ConfigureBuilder.ConfigureBuilderSection;
import com.outbrain.ob1k.server.builder.ResourceMappingBuilder.ResourceMappingBuilderSection;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder.ServiceBindBuilderSection;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder.ServiceRegisterBuilderSection;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ServerBuilderTest {


  @Test
  public void shouldBuildServer() {

// JDK 8 version
//    Server server = ServerBuilder.newBuilder().contextPath("contextPath").
//            resource(r -> r.staticMapping("virtualPath", "realPath").staticPath("path")).
//            service(s -> s.register(new TestService(), "/path",
//                      b -> b.endpoint("testMethod", "/test").endpoint("anotherMethod", "/another"), new TestServiceFilter()).
//                    register(new TestService2(), "/path2")).
//            configure(c -> c.usePort(8080).acceptKeepAlive(true).supportZip(true).requestTimeout(100, TimeUnit.MILLISECONDS)).build();



    Server server = ServerBuilder.newBuilder().
            contextPath("contextPath").
            resource(new ResourceMappingBuilderSection() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void apply(final ResourceMappingBuilder builder) {
                builder.staticMapping("virtualPath", "realPath").staticPath("path");
              }
            }).
            service(new ServiceRegisterBuilderSection() {
              @Override
              public void apply(final ServiceRegisterBuilder builder) {
                // should be lambda to whoever is inJDK 8
                builder.register(new TestService(), "/path", new ServiceBindBuilderSection() {
                  @Override
                  public void apply(final ServiceBindBuilder builder) {
                    builder.endpoint("testMethod", "/test").endpoint("anotherMethod", "/another");
                  }
                }, new TestServiceFilter()).register(new TestService2(), "/path2");
              }
            }).
            configure(new ConfigureBuilderSection() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void apply(final ConfigureBuilder builder) {
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