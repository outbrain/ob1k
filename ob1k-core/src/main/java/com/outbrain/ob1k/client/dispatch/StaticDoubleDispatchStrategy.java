package com.outbrain.ob1k.client.dispatch;

import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureAction;
import rx.Observable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.error;

/**
 * @author hyadid, marenzon
 */
public class StaticDoubleDispatchStrategy implements DispatchStrategy {

  private final long durationMs;

  public StaticDoubleDispatchStrategy(final long durationMs) {
    this.durationMs = durationMs;
  }

  @Override
  public <T> ComposableFuture<T> dispatchAsync(final TargetProvider targetProvider,
                                        final DispatchAction<ComposableFuture<T>> dispatchAction) {
    final List<String> remoteTargets;
    try {
      remoteTargets = targetProvider.provideTargets(2);
    } catch (final RuntimeException e) {
      return fromError(e);
    }

    return ComposableFutures.doubleDispatch(durationMs, MILLISECONDS,
      createDispatchAction(dispatchAction, remoteTargets));
  }

  @Override
  public <T> Observable<T> dispatchStream(final TargetProvider targetProvider,
                                          final DispatchAction<Observable<T>> dispatchAction) {
    final String remoteTarget;
    try {
      remoteTarget = targetProvider.provideTarget();
    } catch (final RuntimeException e) {
      return error(e);
    }

    return dispatchAction.invoke(remoteTarget);
  }

  private <T> FutureAction<T> createDispatchAction(final DispatchAction<ComposableFuture<T>> dispatchAction,
                                            final List<String> remoteTargets) {
    return new FutureAction<T>() {
      private final AtomicInteger targetCount = new AtomicInteger(0);

      @Override
      @SuppressWarnings("unchecked")
      public ComposableFuture<T> execute() {
        final String remoteTarget = remoteTargets.get(targetCount.getAndIncrement());
        return dispatchAction.invoke(remoteTarget);
      }
    };
  }
}