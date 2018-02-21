package com.outbrain.ob1k;

/**
 * a marker interface for a service that can contain a set of endpoints.
 * every endpoint is a method that is bound to a URL that is defined when constructing the server(see {@link com.outbrain.ob1k.server.builder.ServerBuilder})
 *
 * a method must be asynchronous and must return a {@link com.outbrain.ob1k.concurrent.ComposableFuture} or a rx.Observable
 * a method will be used for an endpoint provided it is public and non static.
 * the bounded URL is by default: http://machineName:port/{appContext}/{serviceName}/{methodName}
 * all parts of the URL can be configured by using various bind options on server creation.
 * asynchronous methods must not block the calling thread.
 *
 *
 * @author aronen
 * @since 6/18/13
 */
public interface Service {
}
