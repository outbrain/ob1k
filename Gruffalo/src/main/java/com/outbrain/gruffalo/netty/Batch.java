package com.outbrain.gruffalo.netty;

/**
 * Time: 8/5/13 11:23 AM
 *
 * @author Eran Harel
 */
class Batch {
  final StringBuilder payload;
  final int batchSize;

  Batch(StringBuilder payload, int batchSize) {
    this.payload = payload;
    this.batchSize = batchSize;
  }
}
