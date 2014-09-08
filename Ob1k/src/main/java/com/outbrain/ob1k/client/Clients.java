package com.outbrain.ob1k.client;

import com.outbrain.ob1k.Service;

/**
 * Created by aronen on 7/15/14.
 * helper methods for service clients.
 */
public class Clients {
  public static void close(final Service clientService) throws Exception {
    if (clientService == null)
      return;

    final AutoCloseable service = (AutoCloseable) clientService;
    service.close();
  }
}
