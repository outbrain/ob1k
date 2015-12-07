package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.server.builder.DefaultServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class EndpointMappingServiceBuilderTest {

  @Mock
  private DefaultServerBuilder serverBuilder;
  @Mock
  private ServerBuilderState state;
  @Mock
  private ServiceRegistryView registryView;

  private EndpointMappingServiceBuilder<DefaultServerBuilder> builder;

  @Before
  public void setUp() {
    builder = new EndpointMappingServiceBuilder<>(serverBuilder, state);
  }

  @Test
  public void shouldAddServiceDescriptorForEndpointMappingService() {
    // given
    Mockito.when(state.getRegistry()).thenReturn(registryView);

    // when
    final DefaultServerBuilder nextStep = builder.registerEndpointMappingService("path");

    // then
    Mockito.verify(state).addServiceDescriptor(any(EndpointMappingService.class), eq("path"));
    Assert.assertSame(serverBuilder, nextStep);
  }
}