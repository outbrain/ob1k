package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
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
  private ServerBuilderState state;
  @Mock
  private ServiceRegistryView registryView;

  @Test
  public void shouldAddServiceDescriptorAndMapUiResourcesForSwaggerService() {
    // given
    Mockito.when(state.getRegistry()).thenReturn(registryView);

    // when
    final List<Class<? extends Service>> ignoredServices = Collections.<Class<? extends Service>>singletonList(SwaggerService.class);
    SwaggerServiceBuilder.enableSwagger("path", ignoredServices).provide(state);

    // then
    Mockito.verify(state).addServiceDescriptor(any(SwaggerService.class), eq("path"));
    Mockito.verify(state).addStaticMapping("/" + SwaggerServiceBuilder.SWAGGER_UI_URI, "/META-INF/resources/swagger-ui.html");
  }

}