package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/31/14.
 */
public interface InitialPhase {
  ChooseContextPathPhase configurePorts(PortsProvider provider);
}
