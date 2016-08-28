package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A programmatic API that maps to the /v1/catalog/* consul REST API
 * @author Eran Harel
 */
public interface ConsulCatalog extends Service {
  ComposableFuture<Map<String, Set<String>>> services(final String dc);
  ComposableFuture<Map<String, Set<String>>> findDcLocalServices();
  ComposableFuture<List<ServiceInstance>> findInstances(final String service, final String dc);
  ComposableFuture<List<ServiceInstance>> findDcLocalInstances(final String service);
  ComposableFuture<List<ServiceInstance>> filterDcLocalInstances(final String service, final String filterTag);
  ComposableFuture<List<ServiceInstance>> pollDcLocalInstances(final String service, final String filterTag, final long index, final int maxWaitSec);
  ComposableFuture<Set<String>> datacenters();
}
