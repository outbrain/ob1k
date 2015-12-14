package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ConfigureBuilder;
import com.outbrain.ob1k.server.builder.ConfigureBuilder.ConfigureBuilderSection;
import com.outbrain.ob1k.server.builder.ResourceMappingBuilder;
import com.outbrain.ob1k.server.builder.ResourceMappingBuilder.ResourceMappingBuilderSection;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder;
import com.outbrain.ob1k.server.filters.HitsCounterFilter;
import com.outbrain.ob1k.server.spring.SpringServiceBindBuilder.SpringServiceBindBuilderSection;
import com.outbrain.ob1k.server.spring.SpringServiceRegisterBuilder.SpringServiceRegisterBuilderSection;
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
//            serviceFromContext(s -> s.register("ctx", TestService.class, "/path",
//                    b -> b.endpoint("testMethod", "/test", "ctx", TestServiceFilter.class).endpoint("anotherMethod", "/another", "ctx"), TestServiceFilter.class).
//                    register("ctx", TestService2.class, "/path2")).
//            build();


    Mockito.when(springContext.getBean("ctx", TestService.class)).thenReturn(new TestService());
    Mockito.when(springContext.getBean("ctx", TestService2.class)).thenReturn(new TestService2());
    Mockito.when(springContext.getBean("ctx", HitsCounterFilter.class)).thenReturn(new HitsCounterFilter(metricFactory));

    Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
            configure(new ConfigureBuilderSection() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void apply(final ConfigureBuilder c) {
                c.usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS);
              }
            }).
            resource(new ResourceMappingBuilderSection() {
              // should be lambda to whoever is inJDK 8
              @Override
              public void apply(final ResourceMappingBuilder r) {
                r.staticMapping("virtualPath", "realPath").staticPath("path");
              }
            }).
            serviceFromContext(new SpringServiceRegisterBuilderSection() {
              // should be lambda to whoever is in JDK 8
              @Override
              public void apply(final SpringServiceRegisterBuilder s) {
                s.register("ctx", TestService.class, "/path", new SpringServiceBindBuilderSection() {
                  // should be lambda to whoever is inJDK 8
                  @Override
                  public void apply(final SpringServiceBindBuilder b) {
                    b.endpoint("testMethod", "/test", "ctx", HitsCounterFilter.class).
                            endpoint("anotherMethod", "/another", "ctx").bindPrefix(true).
                            endpoint("testMethod", "/anotherEndpointWithoutCtx", new HitsCounterFilter(metricFactory));
                  }
                }, HitsCounterFilter.class).register("ctx", TestService2.class, "/path2").
                        register(new TestService2(), "/path3", new ServiceBindBuilder.ServiceBindBuilderSection() {
                          @Override
                          public void apply(final ServiceBindBuilder bind) {
                            bind.bindPrefix(true);
                          }
                        });
              }
            }).
            build();

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