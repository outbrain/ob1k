package com.outbrain.ob1k.server;

import java.net.InetSocketAddress;

/**
 * User: aronen
 * Date: 6/27/13
 * Time: 5:06 PM
 */
public interface Server {
  InetSocketAddress start();
  void stop();
  String getContextPath();

  void addListener(Listener listener);
  void removeListener(Listener listener);

  public interface Listener {
    public void serverStarted(Server server);
  }
}
