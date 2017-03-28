package com.outbrain.ob1k.cache.memcache.compression;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CompressionAlgorithmsTest {

  private List<Compressor> compressors;
  private static final byte[] TEST_BYTES = "This is a test string".getBytes(StandardCharsets.UTF_8);

  @Before
  public void setup() {
    compressors = Lists.newArrayList(CompressionAlgorithm.ZSTANDARD.getInstance(), CompressionAlgorithm.LZ4.getInstance(), CompressionAlgorithm.GZIP.getInstance());
  }

  @Test
  public void testCompressors() {
    compressors.forEach(compressor -> Assert.assertArrayEquals(TEST_BYTES, compressor.decompress(compressor.compress(TEST_BYTES))));
  }
}
