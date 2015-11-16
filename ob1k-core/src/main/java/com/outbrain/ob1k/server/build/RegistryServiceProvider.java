package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.registry.ServiceRegistry;

public interface RegistryServiceProvider {
  AddRawServicePhase addServices(final AddRawServicePhase builder, final ServiceRegistry registry, final String path, final ServiceFilter... filters);
}
