package com.outbrain.ob1k.client.dispatch;

import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static rx.Observable.error;

/**
 * The default dispatch strategy, takes new remote target and invokes the endpoints.
 *
 * @author marenzon
 */
public class DefaultDispatchStrategy implements DispatchStrategy {

  public static final DispatchStrategy INSTANCE = new DefaultDispatchStrategy();

  private DefaultDispatchStrategy() {

  }

  @Override
  public <T> ComposableFuture<T> dispatchAsync(final TargetProvider targetProvider, final DispatchAction<ComposableFuture<T>> dispatchAction) {
    final String remoteTarget;
    try {
      remoteTarget = targetProvider.provideTarget();
    } catch (final RuntimeException e) {
      return fromError(e);
    }

    return dispatchAction.invoke(remoteTarget);
  }

  @Override
  public <T> Observable<T> dispatchStream(final TargetProvider targetProvider, final DispatchAction<Observable<T>> dispatchAction) {
    final String remoteTarget;
    try {
      remoteTarget = targetProvider.provideTarget();
    } catch (final RuntimeException e) {
      return error(e);
    }

    return dispatchAction.invoke(remoteTarget);
  }
}
