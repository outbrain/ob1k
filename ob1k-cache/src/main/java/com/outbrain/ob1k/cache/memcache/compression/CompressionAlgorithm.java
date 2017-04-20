package com.outbrain.ob1k.cache.memcache.compression;

public enum CompressionAlgorithm {
  LZ4 {
    @Override
    public Compressor getInstance() {
      return new LZ4Compressor();
    }
  },
  GZIP {
    @Override
    public Compressor getInstance() {
      return new GzipCompressor();
    }
  };

  public abstract Compressor getInstance();

  public static Compressor getDefault() {
    return GZIP.getInstance();
  }
}
