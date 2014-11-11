package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/31/14.
 */
public interface ExtraParamsProvider {
  void configureExtraParams(ExtraParamsPhase builder);
}
