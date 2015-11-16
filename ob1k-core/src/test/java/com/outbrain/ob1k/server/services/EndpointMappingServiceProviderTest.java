package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class EndpointMappingServiceProviderTest {

  private EndpointMappingServiceProvider provider = new EndpointMappingServiceProvider();

  @Mock
  private AddRawServicePhase builder;

  @Mock
  private ServiceRegistry registry;

  @Test
  public void shouldAddEndpointMappingServiceToBuilder() {
    // when
    provider.addServices(builder, registry, "path");

    // then
    Mockito.verify(builder).addService(any(EndpointMappingService.class), eq("path"));
  }
}