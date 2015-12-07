package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.Server;
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

    Mockito.when(springContext.getBean("ctx", TestService.class)).thenReturn(new TestService());
    Mockito.when(springContext.getBean("ctx", TestService2.class)).thenReturn(new TestService2());
    Mockito.when(springContext.getBean("ctx", TestServiceFilter.class)).thenReturn(new TestServiceFilter());

    Server server = SpringContextServerBuilder.newBuilder(springContext).contextPath("contextPath").
            configure().usePort(8080).acceptKeepAlive(true).requestTimeout(100, TimeUnit.MILLISECONDS).and().
            resource().staticMapping("virtualPath", "realPath").staticPath("path").and().
            service().register("ctx", TestService.class, "/path", TestServiceFilter.class).
            endpoint("testMethod", "/test", "ctx", TestServiceFilter.class).endpoint("anotherMethod", "/another", "ctx").
            service().register("ctx", TestService2.class, "/path2").build();

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