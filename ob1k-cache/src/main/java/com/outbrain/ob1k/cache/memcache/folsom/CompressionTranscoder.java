package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.ob1k.cache.memcache.compression.*;
import com.spotify.folsom.Transcoder;

import java.util.Objects;

/**
 * Compression transcoder implementation
 *
 * @author bshushi
 * @param <T>
 * @see com.spotify.folsom.Transcoder
 * @see Compressor
 */
public class CompressionTranscoder<T> implements Transcoder<T> {

  private final Transcoder<T> delegate;
  private final Compressor compressor;

  public CompressionTranscoder(final Transcoder<T> delegate, final Compressor compressor) {
    this.delegate = Objects.requireNonNull(delegate, "delegated transcoder may not be null");
    this.compressor = compressor;
  }

  @Override
  public T decode(final byte[] bytes) {
    return delegate.decode(compressor.decompress(bytes));
  }

  @Override
  public byte[] encode(final T t) {
    return compressor.compress(delegate.encode(t));
  }
}
