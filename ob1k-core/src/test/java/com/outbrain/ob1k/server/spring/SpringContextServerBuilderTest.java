package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.BuilderProvider;
import com.outbrain.ob1k.server.builder.DefaultConfigureBuilder;
import com.outbrain.ob1k.server.builder.DefaultResourceMappingBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SpringContextServerBuilderTest {


  @Mock
  private SpringBeanContext springContext;

  @Test
  public void shouldBuildServer() {

    // JDK 8 Version
//    Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
//            configure(c -> c.usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS)).
//            resource(r -> r.staticMapping("virtualPath", "realPath").staticPath("path")).
//            service(s -> s.register("ctx", TestService.class, "/path",
//                    b -> b.endpoint("testMethod", "/test", "ctx", TestServiceFilter.class).endpoint("anotherMethod", "/another", "ctx"), TestServiceFilter.class).
//                    register("ctx", TestService2.class, "/path2")).
//            build();


    Mockito.when(springContext.getBean("ctx", TestService.class)).thenReturn(new TestService());
    Mockito.when(springContext.getBean("ctx", TestService2.class)).thenReturn(new TestService2());
    Mockito.when(springContext.getBean("ctx", TestServiceFilter.class)).thenReturn(new TestServiceFilter());

    Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
            configure(new BuilderProvider<DefaultConfigureBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final DefaultConfigureBuilder c) {
                c.usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS);
              }
            }).
            resource(new BuilderProvider<DefaultResourceMappingBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final DefaultResourceMappingBuilder r) {
                r.staticMapping("virtualPath", "realPath").staticPath("path");
              }
            }).
            service(new BuilderProvider<SpringServiceRegisterBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final SpringServiceRegisterBuilder s) {
                s.register("ctx", TestService.class, "/path", new BuilderProvider<SpringServiceBindingBuilder>() {
                  // should be lambda to whoever is inJDK 8
                  @Override
                  public void provide(final SpringServiceBindingBuilder b) {
                    b.endpoint("testMethod", "/test", "ctx", TestServiceFilter.class).endpoint("anotherMethod", "/another", "ctx");
                  }
                }, TestServiceFilter.class).register("ctx", TestService2.class, "/path2");
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