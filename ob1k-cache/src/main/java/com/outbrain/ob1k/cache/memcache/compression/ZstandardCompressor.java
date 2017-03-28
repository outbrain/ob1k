package com.outbrain.ob1k.cache.memcache.compression;

import com.github.luben.zstd.Zstd;

public class ZstandardCompressor implements Compressor {

  @Override
  public byte[] compress(final byte[] data) {
    return Zstd.compress(data);
  }

  @Override
  public byte[] decompress(final byte[] data) {
    return Zstd.decompress(data, data.length);
  }
}
