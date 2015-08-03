package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.http.common.ContentType;

import java.util.concurrent.TimeUnit;

/**
 * @author Eran Harel
 */
public class ConsulAPI {

    private static final String AGENT_BASE_URL = "http://localhost:8500/v1/";


    public static ConsulServiceRegistry getServiceRegistry() {
        return ConsulServiceRegistryHolder.INSTANCE;
    }

    public static ConsulCatalog getCatalog() {
        return ConsulCatalogHolder.INSTANCE;
    }

    public static ConsulHealth getHealth() {
        return ConsulHealthHolder.INSTANCE;
    }

    private static class ConsulServiceRegistryHolder {
        private static final ConsulServiceRegistry INSTANCE = createServiceRegistry();

        private static ConsulServiceRegistry createServiceRegistry() {
            return new ClientBuilder<>(ConsulServiceRegistry.class)
                    .setTargetProvider(new SimpleTargetProvider(AGENT_BASE_URL + "agent/service/"))
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
                    .setTargetProvider(new SimpleTargetProvider(AGENT_BASE_URL + "catalog"))
                    .bindEndpoint("findInstances", HttpRequestMethodType.GET, "service/{service}?dc={dc}&stale=true")
                    .bindEndpoint("findDcLocalInstances", HttpRequestMethodType.GET, "service/{service}&stale=true")
                    .bindEndpoint("filterDcLocalInstances", HttpRequestMethodType.GET, "service/{service}?tag={filterTag}&stale=true")
                    .bindEndpoint("pollDcLocalInstances", HttpRequestMethodType.GET, "service/{service}?tag={filterTag}&index={index}&wait={maxWaitSec}s&stale=true")
                    .bindEndpoint("services", HttpRequestMethodType.GET, "services?dc={dc}&stale=true")
                    .setProtocol(ContentType.JSON)
                    .setRequestTimeout(10000)
                    .build();
        }
    }

    private static class ConsulHealthHolder {
        private static final ConsulHealth INSTANCE = createHealth();

        private static ConsulHealth createHealth() {
            final int timeoutSeconds = 30;
            return new ClientBuilder<>(ConsulHealth.class)
                    .setTargetProvider(new SimpleTargetProvider(AGENT_BASE_URL + "health"))
                    .bindEndpoint("filterDcLocalHealthyInstances", HttpRequestMethodType.GET, "service/{service}?passing=true&tag={filterTag}&stale=true")
                    .bindEndpoint("pollHealthyInstances", HttpRequestMethodType.GET, "service/{service}?passing=true&stale=true&tag={filterTag}&index={index}&wait=" + timeoutSeconds + "s")
                    .bindEndpoint("getInstancesHealth", HttpRequestMethodType.GET, "service/{service}?dc={dc}&stale=true")
                    .bindEndpoint("getInstancesChecks", HttpRequestMethodType.GET, "checks/{service}?dc={dc}&stale=true")
                    .bindEndpoint("getInstancesAtState", HttpRequestMethodType.GET, "state/{state}?dc={dc}&stale=true")
                    .setProtocol(ContentType.JSON)
                    .setRequestTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds))
                    .build();
        }
    }

    public static void main(final String[] args) throws Exception {
        System.out.println(getHealth().pollHealthyInstances("MyApp", "environment-dev", 0).get());
        System.out.println(getHealth().filterDcLocalHealthyInstances("MyApp", "environment-dev").get());
//        System.out.println(getCatalog().findDcLocalInstances("MyApp").get());
    }
}
