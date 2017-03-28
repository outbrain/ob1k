package com.outbrain.ob1k.cache.memcache.compression;

import net.spy.memcached.compat.CloseUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.*;

public class GzipCompressor implements Compressor {

  @Override
  public byte[] compress(final byte[] data) {
    if (data == null) {
      throw new NullPointerException("Can't compress null");
    }
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    GZIPOutputStream gz = null;
    try {
      gz = new GZIPOutputStream(bos);
      gz.write(data);
    } catch (final IOException e) {
      throw new RuntimeException("IO exception compressing data", e);
    } finally {
      CloseUtil.close(gz);
      CloseUtil.close(bos);
    }
    byte[] rv = bos.toByteArray();
    return rv;
  }

  @Override
  public byte[] decompress(final byte[] data) {
    ByteArrayOutputStream bos = null;
    if(data != null) {
      final ByteArrayInputStream bis = new ByteArrayInputStream(data);
      bos = new ByteArrayOutputStream();
      GZIPInputStream gis = null;
      try {
        gis = new GZIPInputStream(bis);

        final byte[] buf = new byte[8192];
        int r = -1;
        while ((r = gis.read(buf)) > 0) {
          bos.write(buf, 0, r);
        }
      } catch (final IOException e) {
        bos = null;
      } finally {
        CloseUtil.close(gis);
        CloseUtil.close(bis);
        CloseUtil.close(bos);
      }
    }
    return bos == null ? null : bos.toByteArray();
  }
}
