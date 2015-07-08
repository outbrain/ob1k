package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.common.marshalling.ContentType;

/**
 * @author Eran Harel
 */
public class ConsulAPI {

  public static ConsulServiceRegistry getServiceRegistry() {
    return ConsulServiceRegistryHolder.INSTANCE;
  }

  public static ConsulCatalog getCatalog() {
    return ConsulCatalogHolder.INSTANCE;
  }

  private static class ConsulServiceRegistryHolder {
    private static final ConsulServiceRegistry INSTANCE = createServiceRegistry();

    private static ConsulServiceRegistry createServiceRegistry() {
      return new ClientBuilder<>(ConsulServiceRegistry.class)
              .setTargetProvider(new SimpleTargetProvider("http://localhost:8500/v1/agent/service/"))
              .bindEndpoint("deregister", HttpRequestMethodType.GET, "deregister/{serviceId}")
              .setProtocol(ContentType.JSON)
              .setRequestTimeout(1000)
              .build();
    }
  }

  private static class ConsulCatalogHolder {
    private static final ConsulCatalog INSTANCE = createCatalog();

    private static ConsulCatalog createCatalog() {
      return new ClientBuilder<>(ConsulCatalog.class)
              .setTargetProvider(new SimpleTargetProvider("http://localhost:8500/v1/catalog"))
              .bindEndpoint("findInstances", HttpRequestMethodType.GET, "/service/{service}?dc={dc}")
              .bindEndpoint("findDcLocalInstances", HttpRequestMethodType.GET, "/service/{service}")
              .bindEndpoint("filterDcLocalInstances", HttpRequestMethodType.GET, "/service/{service}?tag={filterTag}")
              .bindEndpoint("pollDcLocalInstances", HttpRequestMethodType.GET, "/service/{service}?tag={filterTag}&index={index}&wait={maxWaitSec}s")
              .bindEndpoint("services", HttpRequestMethodType.GET, "/services?dc={dc}")
              .setProtocol(ContentType.JSON)
              .setRequestTimeout(10000)
              .build();
    }
  }

  public static void main(final String[] args) throws Exception {

    System.out.println(getCatalog().findInstances("MyApp", "dc1").get());
    System.out.println(getCatalog().findDcLocalInstances("MyApp").get());
  }
}
