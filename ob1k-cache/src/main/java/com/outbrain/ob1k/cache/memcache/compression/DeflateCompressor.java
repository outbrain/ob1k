package com.outbrain.ob1k.cache.memcache.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Deflate compressor, using JDK's Deflater
 *
 * @author marenzon
 * @see com.outbrain.ob1k.cache.memcache.compression.Compressor
 * @see java.util.zip.Deflater
 */
public class DeflateCompressor implements Compressor {

  private final int deflateLevel;

  public DeflateCompressor(final int deflateLevel) {
    this.deflateLevel = requireNonNull(deflateLevel, "deflateLevel may not be null");
  }

  @Override
  public byte[] compress(final byte[] data) {
    if (data == null) {
      throw new NullPointerException("compress data is null");
    }

    // estimate compression size, using native lib math
    final int bufferEst = data.length + ((data.length + 7) >> 3) + ((data.length + 63) >> 6) + 5;
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferEst);
    final DeflaterOutputStream deflaterStream = new DeflaterOutputStream(buffer, new Deflater(deflateLevel), 1024);

    try {
      deflaterStream.write(data);
      deflaterStream.close();
      buffer.close();
    } catch (final IOException e) {
      throw new RuntimeException("failed compressing data using deflate", e);
    }

    return buffer.toByteArray();
  }

  @Override
  public byte[] decompress(final byte[] data) {
    if (data == null) {
      return null;
    }

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(data.length);
    final InflaterOutputStream inflaterStream = new InflaterOutputStream(buffer, new Inflater());

    try {
      inflaterStream.write(data);
      inflaterStream.close();
      buffer.close();
    } catch (final IOException e) {
      throw new RuntimeException("failed compressing using deflate", e);
    }

    return buffer.toByteArray();
  }
}