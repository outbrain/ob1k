package com.outbrain.ob1k.client.endpoints;

/**
 * @author marenzon
 */
public interface DispatchAction<T> {

  T invoke(String remoteTarget);
}
