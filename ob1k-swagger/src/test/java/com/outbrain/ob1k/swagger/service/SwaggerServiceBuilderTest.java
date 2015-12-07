package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class SwaggerServiceBuilderTest {

  @Mock
  private DefaultServerBuilder serverBuilder;
  @Mock
  private ServerBuilderState state;
  @Mock
  private ServiceRegistryView registryView;

  private SwaggerServiceBuilder<DefaultServerBuilder> builder;

  @Before
  public void setUp() {
    builder = new SwaggerServiceBuilder<>(serverBuilder, state);
  }

  @Test
  public void shouldAddServiceDescriptorAndMapUiResourcesForSwaggerService() {
    // given
    Mockito.when(state.getRegistry()).thenReturn(registryView);

    // when
    final List<Class<? extends Service>> ignoredServices = Collections.<Class<? extends Service>>singletonList(SwaggerService.class);
    final DefaultServerBuilder nextStep = builder.registerSwaggerService("path", ignoredServices);

    // then
    Mockito.verify(state).addServiceDescriptor(any(SwaggerService.class), eq("path"));
    Mockito.verify(state).addStaticMapping("/" + SwaggerServiceBuilder.SWAGGER_UI_URI, "/META-INF/resources/swagger-ui.html");
    Assert.assertSame(serverBuilder, nextStep);
  }

}