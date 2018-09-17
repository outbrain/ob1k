package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.ResponseHandler;

public interface ServerEndpoint<F extends ServiceFilter> extends ServerEndpointView<F> {

  void invoke(final Request request, final Object[] params, final ResponseHandler handler);

}
