package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.RegistryServiceProvider;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;

public class SwaggerServiceProvider implements RegistryServiceProvider {

  private final Class<? extends Service>[] ignoredServices;

  public SwaggerServiceProvider(Class<? extends Service>... ignoredServices) {
    this.ignoredServices = ignoredServices;
  }

  @Override
  public AddRawServicePhase addServices(AddRawServicePhase builder, ServiceRegistryView registry, String path, ServiceFilter... filters) {
    return builder.addService(new SwaggerService(registry, ignoredServices), path, filters);
  }
}
