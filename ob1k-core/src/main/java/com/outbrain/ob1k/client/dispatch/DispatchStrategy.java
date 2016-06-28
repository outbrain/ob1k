package com.outbrain.ob1k.client.dispatch;

import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.endpoints.EndpointDescription;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

/**
 * Dispatch strategy allows intercepting and defining how to dispatch (execute) a
 * client endpoint request.
 *
 * Examples use-cases would be double dispatch, circuit-breakers or just expanding request information.
 *
 * Each dispatch method will be called upon a call of a client for request, and the dispatch
 * method will be either async or stream depends on the context.
 *
 * @author marenzon
 * @see com.outbrain.ob1k.client.ClientBuilder
 */
public interface DispatchStrategy {

  /**
   * Will be called upon asynchronous mono call of the client, and handles the dispatching
   * of the endpoint
   *
   * @param endpointDescription dispatched client endpoint description
   * @param targetProvider      target provider
   * @param dispatchAction      dispatch action callable
   * @param <T>                 service return type
   * @return a future of request execution
   */
  <T> ComposableFuture<T> dispatchAsync(EndpointDescription endpointDescription, TargetProvider targetProvider,
                                        DispatchAction<ComposableFuture<T>> dispatchAction);

  /**
   * Will be called upon asynchronous stream call of the client, and handles the dispatching
   * of the endpoint
   *
   * @param endpointDescription dispatched client endpoint description
   * @param targetProvider      target provider
   * @param dispatchAction      dispatch action callable
   * @param <T>                 service return type
   * @return an observable of request execution
   */
  <T> Observable<T> dispatchStream(EndpointDescription endpointDescription, TargetProvider targetProvider,
                                   DispatchAction<Observable<T>> dispatchAction);
}