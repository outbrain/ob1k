package com.outbrain.ob1k.server.registry;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;

import java.util.Map;
import java.util.SortedMap;

public interface ServiceRegistryView {
  SortedMap<String,Map<HttpRequestMethodType, ServerEndpointView>> getRegisteredEndpoints();

  String getContextPath();
}
