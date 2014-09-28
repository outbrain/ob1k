package com.outbrain.gruffalo.netty;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This little fella is in charge of pushing back / restoring inbound traffic when needed
 *
 * @author Eran Harel
 */
public class Throttler {

  private static final Logger log = LoggerFactory.getLogger(Throttler.class);

  private final ChannelGroup activeServerChannels;

  public Throttler(final ChannelGroup activeServerChannels) {
    this.activeServerChannels = Preconditions.checkNotNull(activeServerChannels, "activeServerChannels must not be null");
  }

  public void pushBackClients() {
    changeServerAutoRead(false);
  }

  public void restoreClientReads() {
    changeServerAutoRead(true);
  }

  public void changeServerAutoRead(final boolean autoread) {
    log.debug("Setting server autoread={}", autoread);

    boolean serverChannelChanged = false;
    for (final Channel activeServerChannel : activeServerChannels) {
      if (!serverChannelChanged) {
        activeServerChannel.parent().config().setAutoRead(autoread);
        serverChannelChanged = true;
      }

      activeServerChannel.config().setAutoRead(autoread);
    }
  }
}
