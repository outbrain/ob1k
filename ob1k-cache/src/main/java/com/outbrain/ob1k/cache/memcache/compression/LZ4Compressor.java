package com.outbrain.ob1k.cache.memcache.compression;

import net.jpountz.lz4.LZ4Factory;

public class LZ4Compressor implements Compressor {

  private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

  @Override
  public byte[] compress(final byte[] data) {
    return lz4Factory.fastCompressor().compress(data);
  }

  @Override
  public byte[] decompress(final byte[] data) {
    return lz4Factory.safeDecompressor().decompress(data, data.length);
  }
}
