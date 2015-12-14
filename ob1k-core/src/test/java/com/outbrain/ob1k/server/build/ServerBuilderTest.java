package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ServerBuilderTest {


  @Mock
  private PortsProvider portsProvider;

  @Mock
  private RegistryServiceProvider provider;

  @Test
  public void shouldPassTheRegistryToTheServiceProviderSoItCanBuildTheService() {
    final InitialPhase builder = ServerBuilder.newBuilder();

    // when
    builder.configurePorts(portsProvider).
            setContextPath("contextPath").
            withServices(new RawServiceProvider() {
              @Override
              public void addServices(AddRawServicePhase builder) {
                builder.addServices(provider, "path");
              }
            }).build();

    // then
    verify(provider).addServices(any(AddRawServicePhase.class), any(ServiceRegistryView.class), eq("path"));
  }
}
