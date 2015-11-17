package com.outbrain.ob1k.server.registry;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;

import java.util.Map;
import java.util.SortedMap;

public interface ServiceRegistryView {
  SortedMap<String,Map<HttpRequestMethodType,AbstractServerEndpoint>> getRegisteredEndpoints();

  String getContextPath();
}
