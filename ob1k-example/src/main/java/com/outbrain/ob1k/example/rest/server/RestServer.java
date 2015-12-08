package com.outbrain.ob1k.example.rest.server;

import com.outbrain.ob1k.example.rest.server.services.UsersServiceImpl;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ServerBuilder;
import com.outbrain.ob1k.server.services.EndpointMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.HttpRequestMethodType.ANY;
import static com.outbrain.ob1k.HttpRequestMethodType.DELETE;
import static com.outbrain.ob1k.HttpRequestMethodType.GET;
import static com.outbrain.ob1k.HttpRequestMethodType.POST;
import static com.outbrain.ob1k.HttpRequestMethodType.PUT;
import static com.outbrain.ob1k.server.services.EndpointMappingServiceBuilder.registerMappingService;
import static com.outbrain.ob1k.swagger.service.SwaggerServiceBuilder.enableSwagger;
import static java.util.Arrays.asList;

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
    return ServerBuilder.newBuilder().contextPath(CONTEXT_PATH).
            configure(builder -> builder.usePort(port).requestTimeout(50, TimeUnit.MILLISECONDS)).
            service(builder -> builder.register(new UsersServiceImpl(), SERVICE_PATH,
                    bind -> bind.endpoint(GET, "fetchAll", "/").
                            endpoint(POST, "createUser", "/").
                            endpoint(GET, "fetchUser", "/{id}").
                            endpoint(PUT, "updateUser", "/{id}").
                            endpoint(DELETE, "deleteUser", "/{id}").
                            endpoint(ANY, "subscribeChanges", "/subscribe"))).and(registerMappingService("/enpoints")).
            and(enableSwagger("/api/swagger", asList(EndpointMappingService.class))).build();
  }
}