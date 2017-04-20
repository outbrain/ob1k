package com.outbrain.ob1k.cache.memcache.compression;

import net.jpountz.lz4.LZ4Factory;

/**
 * LZ4 compressor, using jpountz' library
 *
 * @author bshushi
 * @see <a href="https://github.com/lz4/lz4-java">lz4</a>
 * @see com.outbrain.ob1k.cache.memcache.compression.Compressor
 */
public class LZ4Compressor implements Compressor {

  private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

  @Override
  public byte[] compress(final byte[] data) {
    if (data == null) {
      throw new NullPointerException("compress data is null");
    }

    return lz4Factory.fastCompressor().compress(data);
  }

  @Override
  public byte[] decompress(final byte[] data) {
    if (data == null) {
      return null;
    }

    return lz4Factory.safeDecompressor().decompress(data, data.length);
  }
}
