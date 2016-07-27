package com.outbrain.ob1k.common.metrics;

import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;

/**
 * register a gauge on the queue size of every event loop belonging to the group.
 */
public class NettyQueuesGaugeBuilder {
  public static void registerQueueGauges(final MetricFactory factory, final EventLoopGroup elg, final String componentName) {
    if (factory == null || elg == null) {
      return;
    }

    int index = 0;
    for (final EventExecutor eventExecutor : elg) {
      if (eventExecutor instanceof SingleThreadEventExecutor) {
        final SingleThreadEventExecutor singleExecutor = (SingleThreadEventExecutor) eventExecutor;
        factory.registerGauge("EventLoopGroup-" + componentName, "EventLoop-" + index, singleExecutor::pendingTasks);

        index++;
      }
    }
  }
}
