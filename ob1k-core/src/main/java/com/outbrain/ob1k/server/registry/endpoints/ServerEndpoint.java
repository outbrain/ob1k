package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.server.ResponseHandler;

public interface ServerEndpoint extends ServerEndpointView {

  void invoke(final Request request, final Object[] params, final ResponseHandler handler);

}
