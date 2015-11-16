package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.InitialPhase;
import com.outbrain.ob1k.server.build.PortsProvider;
import com.outbrain.ob1k.server.build.RawServiceProvider;
import com.outbrain.ob1k.server.build.RegistryServiceProvider;
import com.outbrain.ob1k.server.build.ServerBuilder;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.util.Assert;

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
    verify(provider).addServices(any(AddRawServicePhase.class), any(ServiceRegistry.class), eq("path"));
  }
}
