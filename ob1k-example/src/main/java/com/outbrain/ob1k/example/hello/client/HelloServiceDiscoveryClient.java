package com.outbrain.ob1k.example.hello.client;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.consul.ConsulAPI;
import com.outbrain.ob1k.consul.ConsulBasedTargetProvider;
import com.outbrain.ob1k.consul.HealthyTargetsList;
import com.outbrain.ob1k.example.hello.api.HelloService;
import com.outbrain.ob1k.example.hello.server.HelloServer;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.codahale3.CodahaleMetricsFactory;

/**
 * Basic example of how using Ob1k's RPC client, using our HelloService implementation via consul
 *
 * @author Eran Harel
 */
public class HelloServiceDiscoveryClient {

  private static final int CLIENT_REQUEST_TIMEOUT_MS = 1000;

  public static void main(final String[] args) throws Exception {

    final MetricFactory metricFactory = new CodahaleMetricsFactory(new MetricRegistry());
    final HealthyTargetsList healthyTargetsList = new HealthyTargetsList(ConsulAPI.getHealth(), "Hello", "production", null, metricFactory);
    healthyTargetsList.getInitializationFuture().get();

    // Creating new RPC client, using our service api interface
    final HelloService helloService = new ClientBuilder<>(HelloService.class).
      setProtocol(ContentType.JSON).
      setRequestTimeout(CLIENT_REQUEST_TIMEOUT_MS).
      setTargetProvider(new ConsulBasedTargetProvider(healthyTargetsList, "/hello", null)).
      build();

    while(true) {
      System.out.printf("service returned: '%s'\n", helloService.instance().get());
      Thread.sleep(2000);
    }
  }
}