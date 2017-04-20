package com.outbrain.ob1k.cache.memcache.compression;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertArrayEquals;

@RunWith(MockitoJUnitRunner.class)
public class CompressionAlgorithmsTest {

  private List<Compressor> compressors;
  private static final byte[] TEST_BYTES = "This is a test string".getBytes(StandardCharsets.UTF_8);

  @Before
  public void setup() {
    compressors = newArrayList(CompressionAlgorithm.LZ4.getInstance(), CompressionAlgorithm.GZIP.getInstance());
  }

  @Test
  public void testCompressors() {
    compressors.forEach(compressor -> assertArrayEquals(TEST_BYTES,
      compressor.decompress(compressor.compress(TEST_BYTES))));
  }
}
