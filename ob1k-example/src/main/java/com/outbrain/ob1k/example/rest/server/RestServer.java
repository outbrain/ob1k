package com.outbrain.ob1k.example.rest.server;

import com.outbrain.ob1k.example.rest.server.services.UsersServiceImpl;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.HttpRequestMethodType.*;

/**
 * Rest Server
 *
 * Builds a new ob1k netty server, defining all the endpoints
 * and starting on defined port.
 *
 * @author marenzon
 */
public class RestServer {

  private static final Logger logger = LoggerFactory.getLogger(RestServer.class);

  public static final int PORT = 8080;
  public static final String CONTEXT_PATH = "/api";
  public static final String SERVICE_PATH = "/users";

  Server server;

  public static void main(final String[] args) {
    new RestServer().start(PORT);
  }

  public InetSocketAddress start(final int port) {
    server = buildServer(port);
    final InetSocketAddress address = server.start();
    logger.info("## RestServer is started on port: {} ##", port);
    return address;
  }

  public void stop() {
    if (server != null)
      server.stop();
  }

  public static Server buildServer(final int port) {
    return ServerBuilder.newBuilder().configurePorts(builder -> builder.setPort(port)).
      setContextPath(CONTEXT_PATH).
      withServices(builder -> builder.defineService(new UsersServiceImpl(), SERVICE_PATH, serviceBuilder -> {
        serviceBuilder.addEndpoint(GET, "fetchAll", "/");
        serviceBuilder.addEndpoint(POST, "createUser", "/");
        serviceBuilder.addEndpoint(GET, "fetchUser", "/{id}");
        serviceBuilder.addEndpoint(PUT, "updateUser", "/{id}");
        serviceBuilder.addEndpoint(DELETE, "deleteUser", "/{id}");
        serviceBuilder.addEndpoint(ANY, "subscribeChanges", "/subscribe");
      })).
      configureExtraParams(builder -> builder.setRequestTimeout(50, TimeUnit.MILLISECONDS)).
      build();
  }
}