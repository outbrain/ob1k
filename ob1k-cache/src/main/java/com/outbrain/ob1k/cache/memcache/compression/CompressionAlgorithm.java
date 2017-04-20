package com.outbrain.ob1k.cache.memcache.compression;

import java.util.zip.Deflater;

/**
 * Enum containing available compression algorithms
 * Default one is GZIP.
 *
 * @see Compressor
 */
public enum CompressionAlgorithm {

  DEFAULT {
    @Override
    public Compressor getCompressor() {
      return GZIP.getCompressor();
    }
  },

  LZ4 {
    @Override
    public Compressor getCompressor() {
      return new LZ4Compressor();
    }
  },

  DEFLATE {
    @Override
    public Compressor getCompressor() {
      return new DeflateCompressor(Deflater.FULL_FLUSH); // optimal between speed and compression size
    }
  },

  GZIP {
    @Override
    public Compressor getCompressor() {
      return new GzipCompressor();
    }
  };

  public abstract Compressor getCompressor();
}
