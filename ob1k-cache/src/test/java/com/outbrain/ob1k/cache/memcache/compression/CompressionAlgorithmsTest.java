package com.outbrain.ob1k.cache.memcache.compression;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CompressionAlgorithmsTest {

  private static final byte[] TEST_BYTES = "This is a test string".getBytes(StandardCharsets.UTF_8);

  @Test
  public void testGzip() {
    final GzipCompressor gzipCompressor = new GzipCompressor();
    final byte[] compress = gzipCompressor.compress(TEST_BYTES);

    assertTrue("compressed value cannot be empty", compress.length > 0);
    assertArrayEquals("de-compressed value should be same as origin", TEST_BYTES, gzipCompressor.decompress(compress));
  }

  @Test
  public void testLz4() {
    final LZ4Compressor lz4Compressor = new LZ4Compressor();
    final byte[] compress = lz4Compressor.compress(TEST_BYTES);

    assertTrue("compressed value cannot be empty", compress.length > 0);
    assertArrayEquals("de-compressed value should be same as origin", TEST_BYTES, lz4Compressor.decompress(compress));
  }

  @Test
  public void testDeflate() {
    final DeflateCompressor deflateCompressor = new DeflateCompressor(Deflater.FULL_FLUSH);
    final byte[] compress = deflateCompressor.compress(TEST_BYTES);

    assertTrue("compressed value cannot be empty", compress.length > 0);
    assertArrayEquals("de-compressed value should be same as origin", TEST_BYTES, deflateCompressor.decompress(compress));
  }
}
