package com.outbrain.ob1k.server.build;


/**
 * Created by aronen on 7/16/14.
 */
public interface ChoosePortPhase {
  ChooseContextPathPhase setPort(final int port);
  ChooseContextPathPhase useRandomPort();
}

