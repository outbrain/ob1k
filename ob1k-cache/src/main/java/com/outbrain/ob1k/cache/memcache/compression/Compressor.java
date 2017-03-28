package com.outbrain.ob1k.cache.memcache.compression;

public interface Compressor {

  byte[] compress(byte[] data);

  byte[] decompress(byte[] data);
}
