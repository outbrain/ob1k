package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/31/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface PortsProvider {
  void configure(ChoosePortPhase builder);
}
