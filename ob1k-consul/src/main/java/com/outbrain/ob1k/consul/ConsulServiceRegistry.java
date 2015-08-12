package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.Response;

/**
 * A Consul API simplifying operations for the local module instance.
 *
 * @author Eran Harel
 */
public interface ConsulServiceRegistry extends Service {

  ComposableFuture<String> register(ServiceRegistration serviceRegistrationData);

  ComposableFuture<String> deregister(String serviceId);

  ComposableFuture<Response> enableMaintenance(final String service, final String reason);

  ComposableFuture<Response> disableMaintenance(final String service);
}
