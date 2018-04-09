package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A server listener that registers the service in the consul.
 *
 * @author Eran Harel
 */
public class ConsulServiceRegistrator implements Server.Listener {

  private static final Logger logger = LoggerFactory.getLogger(ConsulServiceRegistrator.class);

  private final ServiceRegistrationDataProvider serviceRegistrationDataProvider;
  private final AtomicBoolean v1 = new AtomicBoolean(true);

  public ConsulServiceRegistrator(final ServiceRegistrationDataProvider serviceRegistrationDataProvider) {
    this.serviceRegistrationDataProvider = Preconditions.checkNotNull(serviceRegistrationDataProvider, "serviceRegistrationDataProvider must not be null");
  }

  @Override
  public void serverStarted(final Server server) {
    final ServiceRegistration serviceRegistration = serviceRegistrationDataProvider.provideServiceRegistrationData(server);

    if (null == serviceRegistration) {
      logger.warn("Service registration is disabled!");
      return;
    }

    registerService(serviceRegistration);
    registerShutdownHook(serviceRegistration);
  }

  private void registerService(final ServiceRegistration registration) {
    logger.info("Registering {} using consul API v1", registration.getID());
    ConsulAPI.getServiceRegistryV1().register(registration).
            map(res -> true).
            recoverWith(throwable -> {
              logger.warn("Failed to register service Using API v1, fallback to v0: {}", throwable);
              return ConsulAPI.getServiceRegistry().register(registration).map(res -> false);
            }).
            consume(responseFuture -> {
              logger.info("{} registration success={}", registration.getID(), responseFuture.isSuccess());
              if (responseFuture.isSuccess()) {
                v1.set(responseFuture.getValue());
              } else {
                logger.warn("Failed to register service: {}", responseFuture.getError());
              }
            });
  }

  private void registerShutdownHook(final ServiceRegistration registration) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        logger.info("Going to deregister service {}", registration.getID());
        try {
          final String deregisterUrl = ConsulAPI.AGENT_BASE_URL + "agent/service/deregister/" + registration.getID();
          final HttpURLConnection urlConnection = (HttpURLConnection) new URL(deregisterUrl).openConnection();
          urlConnection.setRequestMethod(v1.get() ? "PUT" : "GET");
          urlConnection.setConnectTimeout(500);
          urlConnection.setReadTimeout(500);
          urlConnection.getInputStream().close();
          logger.info("Deregistered service {}", registration.getID());
        } catch (final IOException e) {
          logger.error("Failed to deregister service {}", registration.getID(), e);
        }
      }
    });
  }

}
