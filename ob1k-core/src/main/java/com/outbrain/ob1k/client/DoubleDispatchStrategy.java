package com.outbrain.ob1k.client;

import com.outbrain.ob1k.concurrent.Try;

/**
 * An interface which enables to define a custom double dispatch strategy for HttpClient.
 * This mechanism is required in order to ensure the target provider will not use the same target for the two invocation of the double dispatch
 *
 *  @author lifey
 */
public interface DoubleDispatchStrategy {
  /**
   * This method determines how long should the double dispatch delay be.
   * If invocation will be completed before it passes a double dispatch will not be initiated
   * It will be invoked by HttpClient for any RPC call.
   * @return The amount of milliseconds that will pass before double dispatch is initiated
   */
  long getDoubleDispatchIntervalMs();
  /**
   * This method gathers statistics on internal behavior of HttpClient
   * It will be invoked by HttpClient for any RPC call
   * @param result a Try which holds the result of the exception of an invocation (including double dispatch)
   * @param startTimeMs describes when invocation started length of excecution can be deduced by System.currentTimeMillis() - startTimeMs
   */
  void onComplete(Try result, long startTimeMs);
}
