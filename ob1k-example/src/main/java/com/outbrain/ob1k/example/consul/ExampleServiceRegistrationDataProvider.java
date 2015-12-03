package com.outbrain.ob1k.example.consul;

import com.outbrain.ob1k.consul.ServiceRegistration;
import com.outbrain.ob1k.consul.ServiceRegistrationDataProvider;
import com.outbrain.ob1k.server.Server;

import java.util.Objects;
import java.util.Set;

/**
 * An example for a simple {@link ServiceRegistrationDataProvider} implementation.
 * @author Eran Harel
 */
public class ExampleServiceRegistrationDataProvider implements ServiceRegistrationDataProvider {

  private final String checkPath;
  private final int instance;
  private final Set<String> tags;

  public ExampleServiceRegistrationDataProvider(final String checkPath, final Set<String> tags, final int instance) {
    this.checkPath = Objects.requireNonNull(checkPath, "checkPath must not be null");
    this.tags = tags;
    this.instance = instance;
  }

  @Override
  public ServiceRegistration provideServiceRegistrationData(final Server server) {
    final String checkUrl = String.format("http://%s:%d%s%s", "127.0.0.1", server.getPort(), server.getContextPath(), checkPath);
    final ServiceRegistration.Check check = new ServiceRegistration.Check(checkUrl, 1);

    return new ServiceRegistration(server.getApplicationName(), server.getPort(), tags, check, instance);
  }
}
