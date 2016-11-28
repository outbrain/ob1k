package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author Eran Harel
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheLoaderTest {

  private static final String KEY = "key1";
  private static final String VALUE = "val1";

  @Mock
  private TypedCache<String, String> cacheMock;

  @Test
  public void testFromTypedCache_loadSuccess() throws ExecutionException, InterruptedException {
    Mockito.when(cacheMock.getAsync(KEY)).thenReturn(ComposableFutures.fromValue(VALUE));

    final String actualValue = CacheLoader.fromTypedCache(cacheMock).load("meh", KEY).get();
    Assert.assertEquals("loaded value", VALUE, actualValue);
  }

  @Test(expected = ExecutionException.class)
  public void testFromTypedCache_loadFail() throws ExecutionException, InterruptedException {
    Mockito.when(cacheMock.getAsync(KEY)).thenReturn(ComposableFutures.fromError(new RuntimeException("failed to load")));

    CacheLoader.fromTypedCache(cacheMock).load("meh", KEY).get();
  }

  @Test
  public void testFromTypedCache_loadBulkSuccess() throws ExecutionException, InterruptedException {
    final Set<String> keys = Collections.singleton(KEY);
    final Map<String, String> values = Collections.singletonMap(KEY, VALUE);
    Mockito.when(cacheMock.getBulkAsync(keys)).thenReturn(ComposableFutures.fromValue(values));

    final Map<String, String> actualValue = CacheLoader.fromTypedCache(cacheMock).load("meh", keys).get();
    Assert.assertEquals("loaded values", values, actualValue);
  }

  @Test(expected = ExecutionException.class)
  public void testFromTypedCache_loadBulkError() throws ExecutionException, InterruptedException {
    final Set<String> keys = Collections.singleton(KEY);
    Mockito.when(cacheMock.getBulkAsync(keys)).thenReturn(ComposableFutures.fromError(new RuntimeException("failed to load")));

    CacheLoader.fromTypedCache(cacheMock).load("meh", keys).get();
  }
}
