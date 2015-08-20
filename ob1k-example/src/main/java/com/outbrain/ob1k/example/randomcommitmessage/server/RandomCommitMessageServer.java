package com.outbrain.ob1k.example.randomcommitmessage.server;

import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.HttpRequestMethodType.*;

/**
 * Main class for the random commit message server.
 * Sets up a service that will fetch random commit messages from http://whatthecommit.com/
 *
 * @author Eran Harel
 */
public class RandomCommitMessageServer {

  private static final Logger logger = LoggerFactory.getLogger(RandomCommitMessageServer.class);

  public static final int PORT = 8080;
  public static final String CTX_PATH = "/rcm";
  public static final String SERVICE_PATH = "/whatthecommit";

  Server server;

  public static void main(final String[] args) {
    new RandomCommitMessageServer().start(PORT);
  }

  public void start(final int port) {
    server = buildServer(port);
    server.start();
    logger.info("## {} is started on port: {} ##", getClass().getSimpleName(), port);
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }

  private Server buildServer(final int port) {
    return ServerBuilder.newBuilder().
      configurePorts(builder -> builder.setPort(port)).
      setContextPath(CTX_PATH).
      withServices(builder -> builder.defineService(new RandomCommitMessageServiceImpl(), SERVICE_PATH, serviceBuilder -> {
        serviceBuilder.addEndpoint(GET, "single", "/single");
        serviceBuilder.addEndpoint(GET, "multi", "/multi/{numMessages}");
      })).
      configureExtraParams(builder -> builder.setRequestTimeout(2000, TimeUnit.MILLISECONDS)).
      build();
  }

}
