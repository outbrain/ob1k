package com.outbrain.ob1k.server.registry;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryTest {

  private ServiceRegistry registry;

  @Before
  public void setup() {
    registry = new ServiceRegistry();
    registry.setContextPath("path");
  }

  @Test
  public void shouldRegisterServiceMethods() {
    registry.register("name", new MyService(), null, null, false);
    final Map<String, Map<HttpRequestMethodType, ServerEndpointView>> endpoints = registry.getRegisteredEndpoints();
    assertEquals("should be two registered endpoints", 2, endpoints.size());
  }

  @Test
  public void shouldNotRegisterNonPublicMethods() {
    registry.register("name", new MyServiceWithNonPublicMethod(), null, null, false);
    final Map<String, Map<HttpRequestMethodType, ServerEndpointView>> endpoints = registry.getRegisteredEndpoints();
    assertEquals("should be none registered endpoints", 0, endpoints.size());
  }

  @Test
  public void shouldNotRegisterStaticMethods() {
    registry.register("name", new MyServiceWithStaticMethod(), null, null, false);
    final Map<String, Map<HttpRequestMethodType, ServerEndpointView>> endpoints = registry.getRegisteredEndpoints();
    assertEquals("should be none registered endpoints", 0, endpoints.size());
  }

  public static class MyService implements Service {

    public ComposableFuture<Double> handleFloat(final Float param) {
      return null;
    }

    public ComposableFuture<String> returnString() {
      return null;
    }
  }

  public static class MyServiceWithNonPublicMethod implements Service {

    ComposableFuture<String> returnString() {
      return null;
    }
  }

  public static class MyServiceWithStaticMethod implements Service {

    public static ComposableFuture<String> returnString() {
      return null;
    }
  }

}