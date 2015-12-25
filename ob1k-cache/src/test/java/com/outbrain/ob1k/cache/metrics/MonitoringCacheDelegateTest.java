package com.outbrain.ob1k.cache.metrics;

/**
 * @author hunchback
 */

import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.RETURNS_MOCKS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class MonitoringCacheDelegateTest {
  private static final MetricFactory metricFactory = mock(MetricFactory.class, withSettings().defaultAnswer(RETURNS_MOCKS));
  private static final TypedCache<String, String> cache = mock(TypedCache.class);
  private static final TypedCache<String, String> monitor = new MonitoringCacheDelegate<>(cache, MonitoringCacheDelegateTest.class.getSimpleName(), metricFactory);
  private static final String SUCCESS_MSG = "failed success test";
  private static final String ERROR_MSG = "failed error test";
  private static final String TIMEOUT_MSG = "failed timeout test";

  public static class getAsyncTest {
    private final String key = "key1";
    private final String value = "value1";

    @Test
    public void getAsyncSuccessTest() throws ExecutionException, InterruptedException {
      when(cache.getAsync(key)).thenReturn(ComposableFutures.fromValue(value));
      assertEquals(SUCCESS_MSG, monitor.getAsync(key).get(), value);
    }

    @Test(expected = ExecutionException.class)
    public void getAsyncErrorTest() throws ExecutionException, InterruptedException {
      when(cache.getAsync(key)).thenReturn(ComposableFutures.<String>fromError(new InterruptedException(key)));
      monitor.getAsync(key).get();
      fail(ERROR_MSG);
    }

    @Test(expected = ExecutionException.class)
    public void getAsyncTimeoutTest() throws ExecutionException, InterruptedException {
      when(cache.getAsync(key)).thenReturn(ComposableFutures.<String>fromError(new TimeoutException(key)));
      monitor.getAsync(key).get();
      fail(TIMEOUT_MSG);
    }
  }

  public static class GetBulkAsyncTest {
    private final List<String> keys = new ArrayList<>();
    private final Map<String, String> results = new HashMap<>();

    public GetBulkAsyncTest() {
      for (int i = 0; i < 100; i++) {
        keys.add("bulkKey" + i);
        results.put("bulkKey" + i, "value" + i);
      }
    }

    @Test
    public void getBulkAsyncSuccessTest() throws ExecutionException, InterruptedException {
      when(cache.getBulkAsync(keys)).thenReturn(ComposableFutures.fromValue(results));
      assertEquals(SUCCESS_MSG, monitor.getBulkAsync(keys).get(), results);
    }

    @Test(expected = ExecutionException.class)
    public void getBulkAsyncErrorTest() throws ExecutionException, InterruptedException {
      when(cache.getBulkAsync(keys)).thenReturn(ComposableFutures.<Map<String, String>>fromError(new InterruptedException(keys.toString())));
      monitor.getBulkAsync(keys).get();
      fail(ERROR_MSG);
    }

    @Test(expected = ExecutionException.class)
    public void getBulkAsyncTimeoutTest() throws ExecutionException, InterruptedException {
      when(cache.getBulkAsync(keys)).thenReturn(ComposableFutures.<Map<String, String>>fromError(new TimeoutException(keys.toString())));
      monitor.getBulkAsync(keys).get();
      fail(TIMEOUT_MSG);
    }
  }

  public static class SetAsyncTest {
    private final String key = "key1";
    private final String value = "value1";

    @Test
    public void setAsyncSuccessTest() throws ExecutionException, InterruptedException {
      when(cache.setAsync(key, value)).thenReturn(ComposableFutures.fromValue(true));
      assertTrue(SUCCESS_MSG, monitor.setAsync(key, value).get());
    }

    @Test(expected = ExecutionException.class)
    public void setAsyncErrorTest() throws ExecutionException, InterruptedException {
      when(cache.setAsync(key, value)).thenReturn(ComposableFutures.<Boolean>fromError(new InterruptedException(key)));
      monitor.setAsync(key, value).get();
      fail(ERROR_MSG);
    }

    @Test(expected = ExecutionException.class)
    public void setAsyncTimeoutTest() throws ExecutionException, InterruptedException {
      when(cache.setAsync(key, value)).thenReturn(ComposableFutures.<Boolean>fromError(new TimeoutException(key)));
      monitor.setAsync(key, value).get();
      fail(TIMEOUT_MSG);
    }
  }

  public static class SetBulkAsyncTest {
    final Map<String, String> entries = new HashMap<>();
    final Map<String, Boolean> results = new HashMap<>();

    public SetBulkAsyncTest() {
      for (int i = 0; i < 100; i++) {
        entries.put("bulkKey" + i, "value" + i);
        results.put("bulkKey" + i, true);
      }
    }

    @Test
    public void setBulkAsyncSuccessTest() throws ExecutionException, InterruptedException {
      when(cache.setBulkAsync(entries)).thenReturn(ComposableFutures.fromValue(results));
      assertEquals(SUCCESS_MSG, monitor.setBulkAsync(entries).get(), results);
    }

    @Test(expected = ExecutionException.class)
    public void setBulkAsyncErrorTest() throws ExecutionException, InterruptedException {
      when(cache.setBulkAsync(entries)).thenReturn(ComposableFutures.<Map<String, Boolean>>fromError(new InterruptedException(entries.toString())));
      monitor.setBulkAsync(entries).get();
      fail(ERROR_MSG);
    }

    @Test(expected = ExecutionException.class)
    public void setBulkAsyncTimeoutTest() throws ExecutionException, InterruptedException {
      when(cache.setBulkAsync(entries)).thenReturn(ComposableFutures.<Map<String, Boolean>>fromError(new TimeoutException(entries.toString())));
      monitor.setBulkAsync(entries).get();
      fail(TIMEOUT_MSG);
    }
  }

  public static class DeleteAsyncTest {
    final String key = "key1";

    @Test
    public void deleteAsyncSuccessTest() throws ExecutionException, InterruptedException {
      when(cache.deleteAsync(key)).thenReturn(ComposableFutures.fromValue(true));
      assertTrue(SUCCESS_MSG, monitor.deleteAsync(key).get());
    }

    @Test(expected = ExecutionException.class)
    public void deleteAsyncErrorTest() throws ExecutionException, InterruptedException {
      when(cache.deleteAsync(key)).thenReturn(ComposableFutures.<Boolean>fromError(new InterruptedException(key)));
      monitor.deleteAsync(key).get();
      fail(ERROR_MSG);
    }

    @Test(expected = ExecutionException.class)
    public void deleteAsyncTimeoutTest() throws ExecutionException, InterruptedException {
      when(cache.deleteAsync(key)).thenReturn(ComposableFutures.<Boolean>fromError(new TimeoutException(key)));
      monitor.deleteAsync(key).get();
      fail(TIMEOUT_MSG);
    }
  }
}
