package com.outbrain.ob1k.cache.memcache.compression;

/**
 * Compressor Interface
 * Defines all available compression algorithm implementations for the cache library.
 * <p>
 *
 * @see <a href="http://java-performance.info/performance-general-compression/">compressor comperasion</a>
 */
public interface Compressor {

  /**
   * Compress provided value
   *
   * @param data value represented as byte array
   * @return compressed byte array
   */
  byte[] compress(byte[] data);

  /**
   * Decompress provided compressed value
   *
   * @param data compressed byte array
   * @return de-compressed byte array
   */
  byte[] decompress(byte[] data);
}
