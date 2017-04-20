package com.outbrain.ob1k.cache.memcache.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP compressor, using JDK's output stream
 *
 * @author bshushi
 * @see com.outbrain.ob1k.cache.memcache.compression.Compressor
 * @see GZIPOutputStream
 */
public class GzipCompressor implements Compressor {

  @Override
  public byte[] compress(final byte[] data) {
    if (data == null) {
      throw new NullPointerException("compress data is null");
    }

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try {
      final GZIPOutputStream gz = new GZIPOutputStream(buffer);
      gz.write(data);
      gz.close();
      buffer.close();
    } catch (final IOException e) {
      throw new RuntimeException("IO exception compressing data", e);
    }

    return buffer.toByteArray();
  }

  @Override
  public byte[] decompress(final byte[] data) {
    if (data == null) {
      return null;
    }

    final ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    writeIntoBuffer(dataStream, buffer);

    return buffer.toByteArray();
  }

  private void writeIntoBuffer(final ByteArrayInputStream dataStream, final ByteArrayOutputStream buffer) {
    try {
      final GZIPInputStream gzipStream = new GZIPInputStream(dataStream);
      final byte[] buf = new byte[8192];
      int r;
      while ((r = gzipStream.read(buf)) > 0) {
        buffer.write(buf, 0, r);
      }
      dataStream.close();
      buffer.close();
      gzipStream.close();
    } catch (final IOException e) {
      throw new RuntimeException("failed de-compressing using gzip", e);
    }
  }
}
