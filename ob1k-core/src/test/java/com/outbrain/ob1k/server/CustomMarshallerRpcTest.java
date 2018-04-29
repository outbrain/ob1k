package com.outbrain.ob1k.server;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.server.builder.ServerBuilder;
import com.outbrain.ob1k.server.services.SimpleTestService;
import com.outbrain.ob1k.server.services.SimpleTestServiceImpl;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;
import static com.outbrain.swinfra.metrics.DummyMetricFactory.newDummyMetricFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author marenzon
 */
public class CustomMarshallerRpcTest {

  private static Server buildServer(final RequestMarshallerRegistry marshallerRegistry) {
    final Server server = ServerBuilder.newBuilder().
      contextPath("/test").
      configure(builder -> builder.useRandomPort().setMarshallerRegistry(marshallerRegistry).useMetricFactory(newDummyMetricFactory())).
      service(builder -> builder.register(new SimpleTestServiceImpl(), "/simple")).build();

    server.start();

    return server;
  }

  private static SimpleTestService newClient(final int port, final ContentType protocol) {
    return new ClientBuilder<>(SimpleTestService.class).
      setProtocol(protocol).
      setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + "/test/simple")).
      build();
  }

  @Test
  public void testServerWithoutMsgPack() throws Exception {
    final Server server = buildServer(new RequestMarshallerRegistry.Builder().build()); // server without msgpack
    final SimpleTestService testService = newClient(server.getPort(), MESSAGE_PACK);

    try {
      testService.nextRandom().get();
    } catch (final ExecutionException e) {
      assertTrue("request should have fail on status code 415", e.getMessage().contains("status code: 415"));
      return;
    }

    fail("service request should have failed since there's no msgpck marshaller available");
  }
}
