package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.ob1k.cache.memcache.compression.*;
import com.spotify.folsom.Transcoder;

import java.util.Objects;

/**
 * Created by bshushi on 26/03/2017.
 */
public class CompressionTranscoder<T> implements Transcoder<T> {

  private final Transcoder<T> delegate;
  private final Compressor compressor;

  public CompressionTranscoder(final Transcoder<T> delegate, Compressor compressor) {
    this.delegate = Objects.requireNonNull(delegate, "Delegated Transcoder cannot be null.");
    this.compressor = compressor;
  }

  @Override
  public T decode(byte[] bytes) {
    Objects.requireNonNull(bytes, "Decompression data target cannot be null.");
    return delegate.decode(compressor.decompress(bytes));
  }

  @Override
  public byte[] encode(T t) {
    return compressor.compress(delegate.encode(t));
  }
}
