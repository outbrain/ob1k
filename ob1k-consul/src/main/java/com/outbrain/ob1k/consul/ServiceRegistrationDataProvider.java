package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.server.Server;

/**
 * A provider for the {@link ServiceRegistration} data to be registered to consul.
 *
 * @author Eran Harel
 */
public interface ServiceRegistrationDataProvider {

  /**
   * Provides the {@link ServiceRegistration} data to be registered to consul.
   *
   * @param server the server instance in which this service runs
   * @return the ServiceRegistration data or <code>null</code> if no registration is required for this instance
   */
  ServiceRegistration provideServiceRegistrationData(Server server);
}
