package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Histogram;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.spotify.folsom.Transcoder;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Eran Harel
 */
public class ObjectSizeMonitoringTranscoder<T> implements Transcoder<T> {

  private final Histogram getHisto;
  private final Histogram setHisto;
  private final Counter getBytes;
  private final Counter setBytes;

  private final long sampleRate;
  private final Transcoder<T> delegate;

  public ObjectSizeMonitoringTranscoder(final Transcoder<T> transcoderDelegate, final MetricFactory metricFactory, final String cacheName, long sampleRate) {
    this.sampleRate = sampleRate;
    this.delegate = Objects.requireNonNull(transcoderDelegate, "transcoderDelegate must not be null");
    Objects.requireNonNull(metricFactory, "metricFactory must not be null");

    final String baseName = "folsom." + (cacheName == null ? "" : cacheName + ".");
    getHisto = metricFactory.createHistogram(baseName + "get", "objectSize", false);
    setHisto = metricFactory.createHistogram(baseName + "set", "objectSize", false);
    getBytes = metricFactory.createCounter(baseName + "get", "bytes");
    setBytes = metricFactory.createCounter(baseName + "set", "bytes");
  }

  @Override
  public T decode(final byte[] b) {
    if (shouldSampleNow()) {
      final int size = b.length;
      getHisto.update(size);
      getBytes.inc(size);
    }

    return delegate.decode(b);
  }

  @Override
  public byte[] encode(final T t) {
    final byte[] encoded = delegate.encode(t);
    if (shouldSampleNow()) {
      final int size = encoded.length;
      setHisto.update(size);
      setBytes.inc(size);
    }

    return encoded;

  }

  private boolean shouldSampleNow() {
    return ThreadLocalRandom.current().nextLong(sampleRate) == 0;
  }
}
