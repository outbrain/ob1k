package com.outbrain.ob1k.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.build.*;
import com.outbrain.ob1k.server.entities.OtherEntity;
import com.outbrain.ob1k.server.entities.TestEntity;
import com.outbrain.ob1k.server.services.SimpleTestService;
import com.outbrain.ob1k.server.services.SimpleTestServiceImpl;
import junit.framework.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by aronen on 10/6/14.
 */
public class BasicRPCTest {

  @Test
  public void testServerListener() throws Exception {
    Listener listener = new Listener();
    Server server = buildServer(listener);
    server.addListener(listener);
    server.start();
    Assert.assertEquals("serverStarted() wasn't called", 2, listener.serverStartedCallCount);
    server.stop();
  }

  @Test
  public void testServiceCreation() throws Exception {
    Server server = null;
    SimpleTestService client = null;
    try {
      server = buildServer(null);
      final int port = server.start().getPort();
      client = buildClient(port);

      final ComposableFuture<String> res1 =
              client.method1(3, "4", new TestEntity(Sets.newHashSet(1L, 2L, 3L), "moshe", null, Lists.<OtherEntity>newArrayList()));

      try {
        final String response1 = res1.get();
        Assert.assertTrue(response1.endsWith("moshe"));
      } catch (final ExecutionException e) {
        e.printStackTrace();
      }

      final ComposableFuture<TestEntity> res2 = client.method2(3, "4");
      try {
        final TestEntity response2 = res2.get();
        Assert.assertEquals(response2.getOthers().get(0).getValue1(), 1);
        Assert.assertEquals(response2.getOthers().get(0).getValue2(), "2");
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    } finally {
      if (client != null)
        Clients.close(client);

      if (server != null)
        server.stop();
    }

  }

  private SimpleTestService buildClient(final int port) {
    return new ClientBuilder<>(SimpleTestService.class).addTarget("http://localhost:"+port+"/test/simple").build();
  }

  private static Server buildServer(final Listener listener) {
    return ServerBuilder.newBuilder().configurePorts(new PortsProvider() {
      @Override
      public void configure(ChoosePortPhase builder) {
        builder.useRandomPort();
      }
    }).setContextPath("/test").withServices(new RawServiceProvider() {
      @Override
      public void addServices(AddRawServicePhase builder) {
        builder.addService(new SimpleTestServiceImpl(), "/simple");
      }
    }).configureExtraParams(new ExtraParamsProvider() {
      @Override
      public void configureExtraParams(ExtraParamsPhase builder) {
        builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
        if (listener != null) {
          builder.addListener(listener);
        }
      }
    }).build();
  }

  @Test
  public void testSlowService() throws Exception {
    Server server = null;
    SimpleTestService client = null;
    try {
      server = buildServer(null);
      final int port = server.start().getPort();
      client = buildClient(port);

      final ComposableFuture<Boolean> res = client.slowMethod(100);

      try {
        final Boolean response1 = res.get();
        Assert.fail("should get timeout exception");
      } catch (final ExecutionException e) {
        Assert.assertTrue(e.getCause().getMessage().contains("response took too long"));
      }

    } finally {
      if (client != null)
        Clients.close(client);

      if (server != null)
        server.stop();
    }
  }

  @Test
  public void testNoParamMethod() throws Exception {
    Server server = null;
    SimpleTestService client = null;
    try {
      server = buildServer(null);
      final int port = server.start().getPort();
      client = buildClient(port);

      try {
        final Integer nextNum = client.nextRandom().get();
        Assert.assertTrue(nextNum != null);
      } catch (final Exception e) {
        Assert.fail("no params method failed. error: " + e.getMessage());
      }

    } finally {
      if (client != null)
        Clients.close(client);

      if (server != null)
        server.stop();
    }
  }

  private static class Listener implements Server.Listener {

    private int serverStartedCallCount = 0;

    @Override
    public void serverStarted(Server server) {
      serverStartedCallCount++;
    }
  }
}
