package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;

@Deprecated // use new extendable fluent builder in 'builder' package
public interface RegistryServiceProvider {
  AddRawServicePhase addServices(final AddRawServicePhase builder, final ServiceRegistryView registry,
                                 final String path, final ServiceFilter... filters);
}
