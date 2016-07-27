package com.outbrain.ob1k.client;

import com.outbrain.ob1k.Service;
import org.junit.Test;

public class ClientBuilderTest {


  @Test(expected = IllegalArgumentException.class)
  public void shouldBlowUpIfClientTypeIsNotAnInterface() {
    final ClientBuilder<RealService> clientBuilder = new ClientBuilder<>(RealService.class);

    clientBuilder.build();
  }

  private static class RealService implements Service {
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldBlowUpIfClientTypeContainsSyncMethods() {
    final ClientBuilder<WithSync> clientBuilder = new ClientBuilder<>(WithSync.class);

    clientBuilder.build();
  }

  private interface WithSync extends Service {
    String syncMethod();
  }

}
