package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.BeanContext;

/**
 * Created by aronen on 7/31/14.
 */
public interface RawServiceProvider {
  void addServices(final AddRawServicePhase builder);
}
