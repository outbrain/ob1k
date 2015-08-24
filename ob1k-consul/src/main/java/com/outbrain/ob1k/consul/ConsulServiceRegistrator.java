package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    logger.info("Registrating {}", registration.getID());
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
        ConsulAPI.getServiceRegistry().deregister(registration.getID()).consume(new Consumer<String>() {
          @Override
          public void consume(final Try<String> aTry) {
            logger.info("{} deregistration success={}", registration.getID(), aTry.isSuccess());
            if (!aTry.isSuccess()) {
              logger.error("Failed to deregister service:", aTry.getError());
            }
          }
        });
      }
    });
  }

}
