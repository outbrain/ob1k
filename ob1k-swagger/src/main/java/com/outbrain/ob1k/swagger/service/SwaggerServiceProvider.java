package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.RegistryServiceProvider;
import com.outbrain.ob1k.server.registry.ServiceRegistry;

public class SwaggerServiceProvider implements RegistryServiceProvider {

  @Override
  public AddRawServicePhase addServices(AddRawServicePhase builder, ServiceRegistry registry, String path, ServiceFilter... filters) {
    return builder.addService(new SwaggerService(registry), path, filters);
  }
}
