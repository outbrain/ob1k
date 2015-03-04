package com.outbrain.ob1k.server.services;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;
import com.outbrain.ob1k.server.registry.ServiceRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * A service that exposes the path to endpoint mapping.
 *
 * @author Eran Harel
 */
public class EndpointMappingService implements IEndpointMappingService {
  private final ServiceRegistry registry;

  public EndpointMappingService(final ServiceRegistry registry) {
    this.registry = registry;
  }

  public ComposableFuture<SortedMap<String, Map<String, HttpRequestMethodType>>> handle() {
    final SortedMap<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> registeredEndpoints = registry.getRegisteredEndpoints();
    final Function<Map<HttpRequestMethodType, AbstractServerEndpoint>, Map<String, HttpRequestMethodType>> endpointsMap = new Function<Map<HttpRequestMethodType, AbstractServerEndpoint>, Map<String, HttpRequestMethodType>>() {
      @Override
      public Map<String, HttpRequestMethodType> apply(final Map<HttpRequestMethodType, AbstractServerEndpoint> input) {
        final Map<String, HttpRequestMethodType> result = new HashMap<>();
        for (final AbstractServerEndpoint endpoint : input.values()) {
          result.put(endpoint.getTargetAsString(), endpoint.requestMethodType);
        }
        return result;
      }
    };
    return ComposableFutures.fromValue(Maps.transformValues(registeredEndpoints, endpointsMap));
  }
}