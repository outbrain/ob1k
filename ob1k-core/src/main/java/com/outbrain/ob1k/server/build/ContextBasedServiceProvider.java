package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.BeanContext;

/**
 * Created by aronen on 7/30/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface ContextBasedServiceProvider {
  void addServices(final AddServiceFromContextPhase builder, final BeanContext ctx);
}
