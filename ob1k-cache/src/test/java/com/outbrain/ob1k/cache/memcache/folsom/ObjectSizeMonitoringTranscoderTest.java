package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Histogram;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.spotify.folsom.Transcoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eran Harel
 */
@RunWith(MockitoJUnitRunner.class)
public class ObjectSizeMonitoringTranscoderTest {
  public static final int OBJECT_SIZE = 5;
  private static final byte[] DATA = new byte[OBJECT_SIZE];
  private static final Object OBJECT = new Object();
  @Mock
  private Transcoder<Object> delegate;
  @Mock
  private MetricFactory metricFactory;
  @Mock
  private Histogram histogram;
  @Mock
  private Counter counter;
  private ObjectSizeMonitoringTranscoder<Object> objectObjectSizeMonitoringTranscoder;

  @Before
  public void setUp() throws Exception {
    when(metricFactory.createHistogram(anyString(), anyString(), anyBoolean())).thenReturn(histogram);
    when(metricFactory.createCounter(anyString(), anyString())).thenReturn(counter);
    objectObjectSizeMonitoringTranscoder = new ObjectSizeMonitoringTranscoder<>(delegate, metricFactory, "cacheName", 1);
  }

  @After
  public void tearDown() throws Exception {
    delegate = null;
    metricFactory = null;
    histogram = null;
    counter = null;
    objectObjectSizeMonitoringTranscoder = null;
  }

  @Test
  public void testDecode() throws Exception {
    when(delegate.decode(DATA)).thenReturn(OBJECT);
    final Object res = objectObjectSizeMonitoringTranscoder.decode(DATA);

    assertEquals("decoded object", OBJECT, res);
    verify(histogram).update(OBJECT_SIZE);
    verify(counter).inc(OBJECT_SIZE);
  }

  @Test
  public void testEncode() throws Exception {
    when(delegate.encode(OBJECT)).thenReturn(DATA);
    final byte[] res = objectObjectSizeMonitoringTranscoder.encode(OBJECT);

    assertEquals("encoded object", DATA, res);
    verify(histogram).update(OBJECT_SIZE);
    verify(counter).inc(OBJECT_SIZE);
  }

}
