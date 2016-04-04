package com.outbrain.ob1k.cache.memcache.folsom;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.outbrain.swinfra.metrics.api.Meter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import com.spotify.folsom.GetResult;
import com.spotify.folsom.MemcacheStatus;
import com.spotify.folsom.Metrics;
import com.spotify.folsom.client.Utils;

import java.util.List;
import java.util.Objects;

/**
 * Adapts the folsom {@link Metrics} API to the ob1k abstraction.
 *
 * @author Eran Harel
 */
public class MetricsAdapter implements Metrics {

  private final Timer gets;
  private final Meter getHits;
  private final Meter getMisses;

  private final Meter getSuccesses;
  private final Meter getFailures;

  private final Timer multigets;
  private final Meter multigetSuccesses;
  private final Meter multigetFailures;

  private final Timer sets;
  private final Meter setSuccesses;
  private final Meter setFailures;

  private final Timer deletes;
  private final Meter deleteSuccesses;
  private final Meter deleteFailures;

  private final Timer incrDecrs;
  private final Meter incrDecrSuccesses;
  private final Meter incrDecrFailures;

  private final Timer touches;
  private final Meter touchSuccesses;
  private final Meter touchFailures;

  private volatile OutstandingRequestsGauge internalOutstandingReqGauge;

  public MetricsAdapter(final MetricFactory metricFactory, final String cacheName) {
    this.gets = metricFactory.createTimer(cacheName, "get.requests");
    this.getSuccesses = metricFactory.createMeter(cacheName, "get", "successes");
    this.getHits = metricFactory.createMeter(cacheName, "get", "hits");
    this.getMisses = metricFactory.createMeter(cacheName, "get", "misses");
    this.getFailures = metricFactory.createMeter(cacheName, "get", "failures");

    this.multigets = metricFactory.createTimer(cacheName, "multiget.requests");
    this.multigetSuccesses = metricFactory.createMeter(cacheName, "multiget", "successes");
    this.multigetFailures = metricFactory.createMeter(cacheName, "multiget", "failures");

    this.sets = metricFactory.createTimer(cacheName, "set.requests");
    this.setSuccesses = metricFactory.createMeter(cacheName, "set", "successes");
    this.setFailures = metricFactory.createMeter(cacheName, "set", "failures");

    this.deletes = metricFactory.createTimer(cacheName, "delete.requests");
    this.deleteSuccesses = metricFactory.createMeter(cacheName, "delete", "successes");
    this.deleteFailures = metricFactory.createMeter(cacheName, "delete", "failures");

    this.incrDecrs = metricFactory.createTimer(cacheName, "incrdecr.requests");
    this.incrDecrSuccesses = metricFactory.createMeter(cacheName, "incrdecr", "successes");
    this.incrDecrFailures = metricFactory.createMeter(cacheName, "incrdecr", "failures");

    this.touches = metricFactory.createTimer(cacheName, "touch.requests");
    this.touchSuccesses = metricFactory.createMeter(cacheName, "touch", "successes");
    this.touchFailures = metricFactory.createMeter(cacheName, "touch", "failures");

    metricFactory.registerGauge(cacheName, "outstandingRequests.count",
      () -> internalOutstandingReqGauge == null ? 0 : internalOutstandingReqGauge.getOutstandingRequests());
  }

  @Override
  public void measureGetFuture(final ListenableFuture<GetResult<byte[]>> future) {
    final Timer.Context ctx = gets.time();

    final FutureCallback<GetResult<byte[]>> metricsCallback =
      new FutureCallback<GetResult<byte[]>>() {
        @Override
        public void onSuccess(final GetResult<byte[]> result) {
          getSuccesses.mark();
          if (result != null) {
            getHits.mark();
          } else {
            getMisses.mark();
          }
          ctx.stop();
        }

        @Override
        public void onFailure(final Throwable t) {
          getFailures.mark();
          ctx.stop();
        }
      };

    Futures.addCallback(future, metricsCallback, Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void measureMultigetFuture(final ListenableFuture<List<GetResult<byte[]>>> future) {
    final Timer.Context ctx = multigets.time();

    final FutureCallback<List<GetResult<byte[]>>> metricsCallback =
      new FutureCallback<List<GetResult<byte[]>>>() {
        @Override
        public void onSuccess(final List<GetResult<byte[]>> result) {
          multigetSuccesses.mark();
          final long hits = result.stream().filter(Objects::nonNull).count();

          getHits.mark(hits);
          getMisses.mark(result.size() - hits);
          ctx.stop();
        }

        @Override
        public void onFailure(final Throwable t) {
          multigetFailures.mark();
          ctx.stop();
        }
      };

    Futures.addCallback(future, metricsCallback, Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void measureDeleteFuture(final ListenableFuture<MemcacheStatus> future) {
    Futures.addCallback(future, new FutureMutatationCallback(deletes.time(), deleteSuccesses, deleteFailures), Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void measureSetFuture(final ListenableFuture<MemcacheStatus> future) {
    Futures.addCallback(future, new FutureMutatationCallback(sets.time(), setSuccesses, setFailures), Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void measureIncrDecrFuture(final ListenableFuture<Long> future) {
    Futures.addCallback(future, new FutureMutatationCallback(incrDecrs.time(), incrDecrSuccesses, incrDecrFailures), Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void measureTouchFuture(final ListenableFuture<MemcacheStatus> future) {
    Futures.addCallback(future, new FutureMutatationCallback(touches.time(), touchSuccesses, touchFailures), Utils.SAME_THREAD_EXECUTOR);
  }

  @Override
  public void registerOutstandingRequestsGauge(final OutstandingRequestsGauge gauge) {
    this.internalOutstandingReqGauge = gauge;
  }


  private static class FutureMutatationCallback implements FutureCallback<Object> {
    private final Timer.Context ctx;
    private final Meter successes;
    private final Meter failures;

    private FutureMutatationCallback(final Timer.Context ctx, final Meter successes, final Meter failures) {
      this.ctx = ctx;
      this.successes = successes;
      this.failures = failures;
    }

    @Override
    public void onSuccess(final Object result) {
      successes.mark();
      ctx.stop();
    }

    @Override
    public void onFailure(final Throwable t) {
      failures.mark();
      ctx.stop();
    }
  }
}
