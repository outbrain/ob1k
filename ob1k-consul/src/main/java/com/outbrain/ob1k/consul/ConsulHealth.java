package com.outbrain.ob1k.consul;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.TypedResponse;

import java.util.List;

/**
 * A programmatic API that maps to the /v1/health/* consul REST API
 * @author Eran Harel
 */
public interface ConsulHealth extends Service {

    ComposableFuture<List<HealthInfoInstance>> filterDcLocalHealthyInstances(final String service, final String filterTag);
    ComposableFuture<TypedResponse<List<HealthInfoInstance>>> pollHealthyInstances(final String service, final String filterTag, final long index);
}
