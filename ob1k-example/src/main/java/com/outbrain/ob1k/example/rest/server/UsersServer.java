package com.outbrain.ob1k.example.rest.server;

import static com.outbrain.ob1k.HttpRequestMethodType.*;
import com.outbrain.ob1k.example.rest.server.endpoints.UsersService;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;


import java.util.concurrent.TimeUnit;

/**
 * UsersServer class
 * Builds an new ob1k netty server, defining all the endpoints
 * and starting on defined port.
 *
 * @author marenzon
 */
public class UsersServer {
  public static final String CONTEXT_PATH = "/api";
  public static final String SERVICE_PATH = "/users";


  public static Server buildServer(final int port) {
    return ServerBuilder.newBuilder().configurePorts(builder -> {
        builder.setPort(port);
    }).setContextPath(CONTEXT_PATH).withServices(builder -> {
        builder.defineService(new UsersService(), SERVICE_PATH, serviceBuilder -> {
            serviceBuilder.addEndpoint(GET, "getAll", "/");
            serviceBuilder.addEndpoint(GET, "fetchUser", "/{id}");
            serviceBuilder.addEndpoint(POST, "updateUser", "/{id}");
            serviceBuilder.addEndpoint(DELETE, "deleteUser", "/{id}");
            serviceBuilder.addEndpoint(PUT, "createUser", "/");
        });
    }).configureExtraParams(builder -> {
        builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
    }).build();
  }
}