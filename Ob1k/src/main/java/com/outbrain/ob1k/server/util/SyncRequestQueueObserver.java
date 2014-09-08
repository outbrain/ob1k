package com.outbrain.ob1k.server.util;

import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Time: 5/4/14 11:10 AM
 *
 * @author Eran Harel
 */
public class SyncRequestQueueObserver implements QueueObserver {

  private static final Logger log = LoggerFactory.getLogger(SyncRequestQueueObserver.class);
  private final Counter pushbackCounter;
  private final ChannelGroup activeChannels;

  private AtomicBoolean autoRead = new AtomicBoolean(true);

  private volatile Channel serverChannel;

  public SyncRequestQueueObserver(final ChannelGroup activeChannels, final MetricFactory metricFactory) {
    if (metricFactory != null) {
      this.pushbackCounter = metricFactory.createCounter("Ob1kDispatcher", "pushBack");
    } else {
      pushbackCounter = null;
    }

    this.activeChannels = activeChannels;
  }

  public void setServerChannel(final Channel serverChannel) {
    this.serverChannel = serverChannel;
  }

  @Override
  public void onQueueSizeBelowThreshold() {
    if (autoRead.compareAndSet(false, true)) {
      restoreReads();
    }
  }

  private void restoreReads() {
    restoreActiveChannelsReads();
    restoreAccepts();
  }

  private void restoreActiveChannelsReads() {
    log.info("Restoring reads for {} active channels", activeChannels.size());

    for (final Channel activeChannel : activeChannels) {
      activeChannel.config().setAutoRead(true);
    }
  }

  private void restoreAccepts() {
    if (serverChannel == null) {
      log.warn("Server channel is null, and we have no way to restore reads. Are you sure you know what you're doing?");
    } else {
      serverChannel.config().setAutoRead(true);
      log.info("Restored reads for {}", serverChannel.localAddress());
    }
  }

  @Override
  public void onQueueRejection() {
    if (autoRead.compareAndSet(true, false)) {
      pushBackTraffic();
    }
  }

  private void pushBackTraffic() {
    stopAccepts();
    stopActiveChannelReads();
  }

  private void stopActiveChannelReads() {
    for (final Channel activeChannel : activeChannels) {
      activeChannel.config().setAutoRead(false);
    }

    log.info("Stopped reads for {} active channels", activeChannels.size());
  }

  private void stopAccepts() {
    if (serverChannel == null) {
      log.warn("Server channel is null, and we have no way to block reads. Are you sure you know what you're doing?");
    } else {
      serverChannel.config().setAutoRead(false);
      if (pushbackCounter != null) {
        pushbackCounter.inc();
      }

      log.warn("Sync thread pool is rejecting submissions; Pushing back traffic...");
    }
  }

}
