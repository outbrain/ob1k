package com.outbrain.ob1k.example.hello.server;

import com.outbrain.ob1k.example.hello.server.services.HelloServiceImpl;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
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
public class HelloServer {

  private static final Logger logger = LoggerFactory.getLogger(HelloServer.class);

  public static final int PORT = 8080;
  public static final String CTX_PATH = "/services";
  public static final String HELLO_SERVICE_PATH = "/hello";

  Server server;

  public static void main(final String[] args) {
    new HelloServer().start(PORT);
  }

  public InetSocketAddress start(final int port) {
    server = buildServer(port);
    final InetSocketAddress address = server.start();
    logger.info("## HelloServer is started on port: {} ##", port);
    return address;
  }

  public void stop() {
    if (server != null)
      server.stop();
  }

  private Server buildServer(final int port) {
    return ServerBuilder.newBuilder().
            contextPath(CTX_PATH).
            configure(builder -> builder.usePort(port).requestTimeout(50, TimeUnit.MILLISECONDS)).
            service(builder -> builder.register(new HelloServiceImpl(0), HELLO_SERVICE_PATH)).
            build();
  }
}