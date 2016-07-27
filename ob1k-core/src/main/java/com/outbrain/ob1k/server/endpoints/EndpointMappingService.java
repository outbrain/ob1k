package com.outbrain.ob1k.server.endpoints;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * A service that exposes the path to endpoint mapping.
 *
 * @author Eran Harel
 */
public class EndpointMappingService implements IEndpointMappingService {
  private final ServiceRegistryView registry;

  public EndpointMappingService(final ServiceRegistryView registry) {
    this.registry = registry;
  }

  public ComposableFuture<SortedMap<String, Map<String, HttpRequestMethodType>>> handle() {
    final SortedMap<String, Map<HttpRequestMethodType, ServerEndpointView>> registeredEndpoints = registry.getRegisteredEndpoints();
    final Function<Map<HttpRequestMethodType, ServerEndpointView>, Map<String, HttpRequestMethodType>> endpointsMap = input -> {
      final Map<String, HttpRequestMethodType> result = new HashMap<>();
      for (final ServerEndpointView endpoint : input.values()) {
        result.put(endpoint.getTargetAsString(), endpoint.getRequestMethodType());
      }
      return result;
    };
    return ComposableFutures.fromValue(Maps.transformValues(registeredEndpoints, endpointsMap));
  }
}