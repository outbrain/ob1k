package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.RegistryServiceProvider;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;

public class EndpointMappingServiceProvider implements RegistryServiceProvider {

  @Override
  public AddRawServicePhase addServices(AddRawServicePhase builder, ServiceRegistryView registry, String path, ServiceFilter... filters) {
    return builder.addService(new EndpointMappingService(registry), path, filters);
  }
}
