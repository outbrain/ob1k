package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.BeanContext;
import com.outbrain.ob1k.server.Server;

/**
 * Created by aronen on 7/16/14.
 */
public interface ChooseServiceCreationTypePhase {
  ChooseServiceCreationTypePhase withServicesFrom(final BeanContext ctx, final ContextBasedServiceProvider provider);
  ChooseServiceCreationTypePhase withServices(final RawServiceProvider provider);

  ChooseServiceCreationTypePhase configureStaticResources(StaticResourcesProvider provider);
  ChooseServiceCreationTypePhase configureExtraParams(ExtraParamsProvider provider);

  Server build();
}
