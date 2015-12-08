package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.BuilderProvider;
import com.outbrain.ob1k.server.build.ConfigureBuilder;
import com.outbrain.ob1k.server.build.ResourceMappingBuilder;
import com.outbrain.ob1k.server.filters.HitsCounterFilter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
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
  @Mock
  private MetricFactory metricFactory;

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
    Mockito.when(springContext.getBean("ctx", HitsCounterFilter.class)).thenReturn(new HitsCounterFilter(metricFactory));

    Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
            configure(new BuilderProvider<ConfigureBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final ConfigureBuilder c) {
                c.usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS);
              }
            }).
            resource(new BuilderProvider<ResourceMappingBuilder>() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void provide(final ResourceMappingBuilder r) {
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
                    b.endpoint("testMethod", "/test", "ctx", HitsCounterFilter.class).endpoint("anotherMethod", "/another", "ctx");
                  }
                }, HitsCounterFilter.class).register("ctx", TestService2.class, "/path2");
              }
            }).build();

    Assert.assertEquals("contextPath", server.getContextPath());
  }

  private class TestService implements Service {

    public ComposableFuture<String> testMethod() {
      return null;
    }

    public ComposableFuture<String> anotherMethod() {
      return null;
    }
  }

  private class TestService2 implements Service {
  }
}