package com.outbrain.ob1k.server.registry;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.common.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static com.outbrain.ob1k.http.common.ContentType.JSON;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryTest {

  @Mock
  private RequestMarshallerRegistry marshallerRegistry;

  private ServiceRegistry registry;

  @Before
  public void setup() {
    registry = new ServiceRegistry();
    registry.setMarshallerRegistry(marshallerRegistry);
    registry.setContextPath("path");
  }

  @Test
  public void shouldRegisterServiceMethods() {
    registry.register("name", new MyService(), null, null, false);

    verify(marshallerRegistry).registerTypes(Float.class, Double.class);
    verify(marshallerRegistry).registerTypes(String.class);
  }

  @Test
  public void shouldNotRegisterNonPublicMethods() {
    registry.register("name", new MyServiceWithNonPublicMethod(), null, null, false);

    verify(marshallerRegistry, never()).registerTypes(String.class);
  }

  @Test
  public void shouldNotRegisterNonComposableFutureMethods() {
    registry.register("name", new MyServiceWithNonComposableFutureMethod(), null, null, false);

    verify(marshallerRegistry).registerTypes(Float.class, Double.class);
    verify(marshallerRegistry).registerTypes(Long.class);
    verify(marshallerRegistry, never()).registerTypes(String.class);
  }

  @Test
  public void shouldNotRegisterStaticMethods() {
    registry.register("name", new MyServiceWithStaticMethod(), null, null, false);

    verify(marshallerRegistry, never()).registerTypes(String.class);
  }

  @Test
  public void testServiceRegistryWithCustomMarshaller(){
    final RequestMarshaller mockMarshaller = mock(RequestMarshaller.class);
    final Map<String, RequestMarshaller> marshallers = singletonMap(JSON.requestEncoding(), mockMarshaller);
    final RequestMarshallerRegistry customMarshallerRegistry = new RequestMarshallerRegistry(marshallers);

    registry.setMarshallerRegistry(customMarshallerRegistry);
    registry.register("name", new MyService(), null, null, false);

    verify(mockMarshaller).registerTypes(Float.class, Double.class);
    verify(mockMarshaller).registerTypes(String.class);
  }


  public static class MyService implements Service {

    public ComposableFuture<Double> handleFloat(final Float param) {
      return null;
    }

    public ComposableFuture<String> returnString() {
      return null;
    }
  }

  public static class MyServiceWithNonComposableFutureMethod implements Service {
    public ComposableFuture<Double> handleFloat(final Float param) {
      return null;
    }
    public ComposableFuture<Long> returnLong() {
      return null;
    }
    public String returnStringWithoutComposableFuture() {
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