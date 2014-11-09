package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;

import java.util.List;

/**
 * Created by aronen on 7/16/14.
 *
 * add services to the server defined in the context.
 */
public interface AddServiceFromContextPhase extends AddRawServicePhase {
  AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                   final String path);

  AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                   final Class<? extends ServiceFilter> filterType, final String path);

  AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                   final Class<? extends ServiceFilter> filterType,
                                                   final String path, final boolean bindPrefix);

  AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                   final List<Class<? extends ServiceFilter>> filterTypes,
                                                   final String path, final boolean bindPrefix);

  AddServiceFromContextPhase defineServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                      final String path, final ContextBasedServiceBindingProvider provider);

  AddServiceFromContextPhase defineServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                      final String path, final boolean bindPrefix,
                                                      final ContextBasedServiceBindingProvider provider);

  AddServiceFromContextPhase addServices(ContextBasedServiceProvider provider);

}
