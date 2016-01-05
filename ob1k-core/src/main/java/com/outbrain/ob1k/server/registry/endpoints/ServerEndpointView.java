package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

import java.lang.reflect.Method;

public interface ServerEndpointView<F extends ServiceFilter> {

    String getTargetAsString();

    HttpRequestMethodType getRequestMethodType();

    Method getMethod();

    String[] getParamNames();

    F[] getFilters();
}
