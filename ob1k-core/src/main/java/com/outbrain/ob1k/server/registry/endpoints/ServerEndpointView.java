package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;

import java.lang.reflect.Method;

public interface ServerEndpointView {

    String getTargetAsString();

    HttpRequestMethodType getRequestMethodType();

    Method getMethod();

    String[] getParamNames();
}
