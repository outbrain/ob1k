package com.outbrain.ob1k.example.rest;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.example.rest.endpoints.UsersService;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.ChoosePortPhase;
import com.outbrain.ob1k.server.build.ExtraParamsPhase;
import com.outbrain.ob1k.server.build.ExtraParamsProvider;
import com.outbrain.ob1k.server.build.PortsProvider;
import com.outbrain.ob1k.server.build.RawServiceBuilderPhase;
import com.outbrain.ob1k.server.build.RawServiceProvider;
import com.outbrain.ob1k.server.build.ServerBuilder;
import com.outbrain.ob1k.server.build.ServiceBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * RestServer class
 * Builds an new ob1k netty server, defining all the endpoints
 * and starting on defined port.
 *
 * @author marenzon
 */
public class RestServer {

  private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
  private static final int PORT = 8080;

  public static void main(final String[] args) {

    final Server server = buildServer(PORT);
    server.start();

    logger.info("** RestServer Have Been Started On Port: {} **", PORT);
  }

  private static Server buildServer(final int port) {

    return ServerBuilder.newBuilder().configurePorts(new PortsProvider() {
      @Override
      public void configure(final ChoosePortPhase builder) {
        builder.setPort(port);
      }
    }).setContextPath("/api").withServices(new RawServiceProvider() {
      @Override
      public void addServices(final AddRawServicePhase builder) {
        builder.defineService(new UsersService(), "/users", new ServiceBindingProvider() {
          @Override
          public void configureService(final RawServiceBuilderPhase builder) {
            builder.addEndpoint(HttpRequestMethodType.GET, "getAll", "/");
            builder.addEndpoint(HttpRequestMethodType.GET, "fetchUser", "/{id}");
            builder.addEndpoint(HttpRequestMethodType.POST, "updateUser", "/{id}");
            builder.addEndpoint(HttpRequestMethodType.DELETE, "deleteUser", "/{id}");
            builder.addEndpoint(HttpRequestMethodType.PUT, "createUser", "/");
          }
        });
      }
    }).configureExtraParams(new ExtraParamsProvider() {
      @Override
      public void configureExtraParams(final ExtraParamsPhase builder) {
        builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
      }
    }).build();
  }
}