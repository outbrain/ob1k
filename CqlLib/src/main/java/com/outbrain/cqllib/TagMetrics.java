package com.outbrain.cqllib;

import com.google.common.base.Preconditions;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Created by guyk on 7/9/14.
 */
public class TagMetrics {


  final String tag;
  final Counter writeTimeouts;
  final Counter readTimeouts;
  final Counter unavailableTimeouts;
  final Counter errors;
  //final Histogram resultSetSize;
  final Timer timer;

  TagMetrics(final MetricFactory metricFactory, final String tag) {
    this.tag = Preconditions.checkNotNull(tag);
    readTimeouts = metricFactory.createCounter("CQL", tag + ".readTimeouts");
    writeTimeouts = metricFactory.createCounter("CQL", tag + ".writeTimeouts");
    unavailableTimeouts = metricFactory.createCounter("CQL", tag + ".unavailableTimeouts");
    errors = metricFactory.createCounter("CQL", tag + ".errors");
    timer = metricFactory.createTimer("CQL", tag + ".timer");
    //resultSetSize = metricFactory.createHistogram("CQL", tag + ".resultSetSize", false);
  }

  @Override
  public String toString() {
    return tag;
  }
}
