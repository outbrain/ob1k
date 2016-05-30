package com.outbrain.ob1k.client.dispatch;

import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

/**
 * @author marenzon
 */
public interface DispatchStrategy {

  <T> ComposableFuture<T> dispatchAsync(TargetProvider targetProvider, DispatchAction<ComposableFuture<T>> dispatchAction);
  <T> Observable<T> dispatchStream(TargetProvider targetProvider, DispatchAction<Observable<T>> dispatchAction);
}