package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder;
import com.outbrain.ob1k.server.filters.HitsCounterFilter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static com.outbrain.swinfra.metrics.DummyMetricFactory.newDummyMetricFactory;

@RunWith(MockitoJUnitRunner.class)
public class SpringContextServerBuilderTest {


  @Mock
  private SpringBeanContext springContext;
  @Mock
  private MetricFactory metricFactory;

  @Test
  public void shouldBuildServer() {
    Mockito.when(springContext.getBean("ctx", TestService.class)).thenReturn(new TestService());
    Mockito.when(springContext.getBean("ctx", TestService2.class)).thenReturn(new TestService2());
    Mockito.when(springContext.getBean("ctx", HitsCounterFilter.class)).thenReturn(new HitsCounterFilter(metricFactory));

    final Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
            configure(builder -> builder.usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS).useMetricFactory(newDummyMetricFactory())).
            resource(builder -> builder.staticMapping("virtualPath", "realPath").staticPath("path")).
            serviceFromContext(builder -> builder.register("ctx", TestService.class, "/path",
                bind -> bind.endpoint("testMethod", "/test", "ctx", HitsCounterFilter.class)
                  .endpoint("anotherMethod", "/another", "ctx").bindPrefix(true)
                  .endpoint("testMethod", "/anotherEndpointWithoutCtx", new HitsCounterFilter(metricFactory)))
              .withFilters("ctx", HitsCounterFilter.class)
              .register("ctx", TestService2.class, "/path2")
              .register(new TestService2(), "/path3", (ServiceBindBuilder.ServiceBindBuilderSection)
                bind -> bind.bindPrefix(true)))
            .build();

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
