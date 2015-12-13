package com.outbrain.ob1k.example.hello.server;

import com.google.common.collect.Sets;
import com.outbrain.ob1k.consul.ConsulServiceRegistrator;
import com.outbrain.ob1k.example.consul.ExampleServiceRegistrationDataProvider;
import com.outbrain.ob1k.example.hello.server.services.HelloServiceImpl;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Hello Server
 *
 * This class is our main, which initializing a new server,
 * registering our services and binding on port 8080
 *
 * Here we're basically describing which services do we want to register,
 * how to configure the server and set define the routing
 *
 * @author marenzon
 */
public class HelloServerWithConsulRegistration {

  private static final Logger logger = LoggerFactory.getLogger(HelloServerWithConsulRegistration.class);

  public static final int PORT_BASE = 8080;
  public static final String CTX_PATH = "/api";
  public static final String SERVICE_PATH = "/hello";

  private Server server;
  private final int instance;
  private final int port;

  public HelloServerWithConsulRegistration(final int instance) {
    this.instance = instance;
    this.port = PORT_BASE + instance;
  }

  public InetSocketAddress start() {
    server = buildServer();
    final InetSocketAddress address = server.start();
    logger.info("## HelloServer is started on port: {} ##", port);
    return address;
  }

  public void stop() {
    if (server != null)
      server.stop();
  }

  private Server buildServer() {
    return ServerBuilder.newBuilder().
      configurePorts(builder -> builder.setPort(port)).
      setContextPath(CTX_PATH, "Hello").
      withServices(builder -> builder.addService(new HelloServiceImpl(instance), SERVICE_PATH)).
      configureExtraParams(builder -> {
        builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
        final Set<String> tags = Sets.newHashSet("instance" + instance, "production");
        builder.addListener(new ConsulServiceRegistrator(new ExampleServiceRegistrationDataProvider(SERVICE_PATH + "/instance", tags, instance)));
      }).
      build();
  }

  public static void main(final String[] args) {
    final int instance = Integer.parseInt(args[0]);
    new HelloServerWithConsulRegistration(instance).start();
  }
}