package com.outbrain.ob1k.server.services;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;
import com.outbrain.ob1k.server.registry.ServiceRegistry;

import java.util.SortedMap;

/**
 * A service that exposes the path to endpoint mapping.
 *
 * @author Eran Harel
 */
public class EndpointMappingService implements Service {

  private final ServiceRegistry registry;

  public EndpointMappingService(final ServiceRegistry registry) {
    this.registry = registry;
  }

  public ComposableFuture<SortedMap<String, String>> handle() {
    final SortedMap<String, AbstractServerEndpoint> registeredEndpoints = registry.getRegisteredEndpoints();
    final Function<AbstractServerEndpoint, String> endpoint2string = new Function<AbstractServerEndpoint, String>() {
      @Override
      public String apply(final AbstractServerEndpoint endpoint) {
        return endpoint.getTargetAsString();
      }
    };
    return ComposableFutures.fromValue(Maps.transformValues(registeredEndpoints, endpoint2string));
  }
}
