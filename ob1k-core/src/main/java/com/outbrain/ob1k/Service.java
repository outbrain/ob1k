package com.outbrain.ob1k;

import com.outbrain.ob1k.server.build.ServerBuilder;

/**
 * a marker interface for a service that can contain a set of endpoints. <br/>
 * every endpoint is a method that is bound to a URL that is defined when constructing the server(see {@link ServerBuilder}) <br/>
 *
 * a method can be either synchronous or asynchronous depending on the return type. async method must return a {@link com.outbrain.ob1k.concurrent.ComposableFuture}. <br/>
 * a method will be used for an endpoint provided it is public and non static.
 * the bounded URL is by default: http://machineName:port/{appContext}/{serviceName}/{methodName} <br/>
 * all parts of the URL can be configured by using various bind options on server creation.<br/>
 * asynchronous methods must not block the calling thread.
 * <p/>
 *
 * @author aronen
 * @since 6/18/13
 */
public interface Service {
}
