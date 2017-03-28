package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.ob1k.cache.memcache.compression.Compressor;
import com.spotify.folsom.Transcoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by bshushi on 26/03/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class CompressionTranscoderTest {

  private CompressionTranscoder<String> compressionTranscoder;
  private String DATA = "This is a string";

  @Mock
  Transcoder<String> transcoder;

  @Mock
  Compressor compressor;

  @Before
  public void setup() {
    Mockito.when(transcoder.encode(DATA)).thenReturn(DATA.getBytes());
    Mockito.when(transcoder.decode(DATA.getBytes())).thenReturn(DATA);
    Mockito.when(compressor.compress(DATA.getBytes())).thenReturn(DATA.getBytes());
    Mockito.when(compressor.decompress(DATA.getBytes())).thenReturn(DATA.getBytes());
    compressionTranscoder = new CompressionTranscoder<>(transcoder, compressor);
  }

  @Test
  public void testCompressionTranscoder() {
    String decoded = compressionTranscoder.decode(compressionTranscoder.encode(DATA));
    Assert.assertEquals(DATA, decoded);
    Mockito.verify(compressor).compress(DATA.getBytes());
    Mockito.verify(transcoder).encode(DATA);
    Mockito.verify(compressor).decompress(DATA.getBytes());
    Mockito.verify(transcoder).decode(DATA.getBytes());
  }
}
