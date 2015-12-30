package com.outbrain.ob1k.server.registry;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Executors;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryTest {

  @Mock
  private RequestMarshallerRegistry marshallerRegistry;

  private ServiceRegistry registry;

  @Before
  public void setup() {
    registry = new ServiceRegistry(marshallerRegistry);
    registry.setContextPath("path");
  }

  @Test
  public void shouldRegisterServiceMethods() {
    registry.register("name", new MyService(), false, Executors.newSingleThreadExecutor());

    verify(marshallerRegistry).registerTypes(Float.class, Double.class);
    verify(marshallerRegistry).registerTypes(String.class);
  }

  @Test
  public void shouldNotRegisterNonPublicMethods() {
    registry.register("name", new MyServiceWithNonPublicMethod(), false, Executors.newSingleThreadExecutor());

    verify(marshallerRegistry, never()).registerTypes(String.class);
  }

  @Test
  public void shouldNotRegisterStaticMethods() {
    registry.register("name", new MyServiceWithStaticMethod(), false, Executors.newSingleThreadExecutor());

    verify(marshallerRegistry, never()).registerTypes(String.class);
  }

  public static class MyService implements Service {

    public ComposableFuture<Double> handleFloat(Float param) {
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