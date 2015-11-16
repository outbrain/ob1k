package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.server.Server;

/**
 * adding raw services to the server.
 * @author aronen on 7/16/14.
 */
public interface AddRawServicePhase {
  AddRawServicePhase addService(final Service service, final String path, final ServiceFilter... filters);
  AddRawServicePhase addService(final Service service, final String path, final boolean bindPrefix, final ServiceFilter... filters);

  AddRawServicePhase defineService(final Service service, final String path, ServiceBindingProvider provider);
  AddRawServicePhase defineService(final Service service, final String path, final boolean bindPrefix, ServiceBindingProvider provider);

  AddRawServicePhase addServices(RawServiceProvider provider);
  AddRawServicePhase addServices(RegistryServiceProvider provider, String path, ServiceFilter... filters);

}
