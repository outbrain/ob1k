package com.outbrain.ob1k.server.util;

/**
 * Time: 5/2/14 9:19 PM
 *
 * @author Eran Harel
 */
public interface QueueObserver {
  void onQueueSizeBelowThreshold();
  void onQueueRejection();
}
