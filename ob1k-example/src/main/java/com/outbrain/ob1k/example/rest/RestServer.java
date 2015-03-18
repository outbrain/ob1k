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
    return ServerBuilder.newBuilder().configurePorts(builder -> {
        builder.setPort(port);
    }).setContextPath("/api").withServices(builder -> {
        builder.defineService(new UsersService(), "/users", serviceBuilder -> {
            serviceBuilder.addEndpoint(HttpRequestMethodType.GET, "getAll", "/");
            serviceBuilder.addEndpoint(HttpRequestMethodType.GET, "fetchUser", "/{id}");
            serviceBuilder.addEndpoint(HttpRequestMethodType.POST, "updateUser", "/{id}");
            serviceBuilder.addEndpoint(HttpRequestMethodType.DELETE, "deleteUser", "/{id}");
            serviceBuilder.addEndpoint(HttpRequestMethodType.PUT, "createUser", "/");
        });
    }).configureExtraParams(builder -> {
        builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
    }).build();
  }
}