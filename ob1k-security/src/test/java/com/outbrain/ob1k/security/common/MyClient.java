package com.outbrain.ob1k.security.common;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;

/**
 * Created by gmarom on 6/24/15
 */
public class MyClient {

  public static <T extends Service> T newClient(final int port,
                                                final Class<T> serviceClass) {
    return new ClientBuilder<>(serviceClass)
      .setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + MyServer.CONTEXT_PATH + serviceClass.getSimpleName()))
      .setRequestTimeout(3000) //Take into account busy build machines
      .build();
  }

}
