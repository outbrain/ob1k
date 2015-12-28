package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A server listener that registers the service in the consul.
 *
 * @author Eran Harel
 */
public class ConsulServiceRegistrator implements Server.Listener {

  private static final Logger logger = LoggerFactory.getLogger(ConsulServiceRegistrator.class);

  private final ServiceRegistrationDataProvider serviceRegistrationDataProvider;

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
    logger.info("Registering {}", registration.getID());
    ConsulAPI.getServiceRegistry().register(registration).consume(new Consumer<String>() {
      @Override
      public void consume(final Try<String> responseFuture) {
        logger.info("{} registration success={}", registration.getID(), responseFuture.isSuccess());
        if (!responseFuture.isSuccess()) {
          logger.warn("Failed to register service: {}", responseFuture.getError().toString());
        }
      }
    });
  }

  private void registerShutdownHook(final ServiceRegistration registration) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        logger.info("Going to deregister service {}", registration.getID());
        try {
          final URLConnection urlConnection = new URL("http://localhost:8500/v1/agent/service/deregister/" + registration.getID()).openConnection();
          urlConnection.setConnectTimeout(1000);
          urlConnection.getInputStream().close();
          logger.info("Deregistered service {}", registration.getID());
        } catch (final IOException e) {
          logger.error("Failed to deregister service {}", registration.getID(), e);
        }
      }
    });
  }

}
