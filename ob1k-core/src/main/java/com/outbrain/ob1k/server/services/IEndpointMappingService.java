package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;
import java.util.SortedMap;

/**
 * Created by aronen on 3/1/15.
 */
public interface IEndpointMappingService extends Service {
    public ComposableFuture<SortedMap<String, Map<String, HttpRequestMethodType>>> handle();
}
