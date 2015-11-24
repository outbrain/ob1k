package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class SwaggerServiceProviderTest {

  private SwaggerServiceProvider provider;

  @Mock
  private AddRawServicePhase builder;

  @Mock
  private ServiceRegistryView registry;

  @Test
  public void shouldAddEndpointMappingServiceToBuilder() {
    // given
    provider = new SwaggerServiceProvider();
    // when
    provider.addServices(builder, registry, "path");

    // then
    Mockito.verify(builder).addService(any(SwaggerService.class), eq("path"));
  }

  @Test
  public void shouldConfigureIgnoredServicesInCreatedSwaggerService() {
    // given
    provider = new SwaggerServiceProvider(IgnoredService.class);
    ArgumentCaptor<SwaggerService> serviceCaptor = forClass(SwaggerService.class);
    // when
    provider.addServices(builder, registry, "path");

    // then
    Mockito.verify(builder).addService(serviceCaptor.capture(), eq("path"));

    assertTrue(serviceCaptor.getValue().getIgnoredServices().contains(IgnoredService.class));
  }
}