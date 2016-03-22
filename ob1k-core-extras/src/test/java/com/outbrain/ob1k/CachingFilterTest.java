package com.outbrain.ob1k;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.server.filters.CachingFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Eran Harel
 */
@RunWith(MockitoJUnitRunner.class)
public class CachingFilterTest {

  private static final CachingFilter.CacheKeyGenerator<String> KEY_GENERATOR = params -> String.valueOf(params[0]);
  private static final ComposableFuture<Object> VALUE1 = ComposableFutures.fromValue(1);
  private static final ComposableFuture<Object> VALUE2 = ComposableFutures.fromValue(2);
  private static final ComposableFuture<Object> VALUE3 = ComposableFutures.fromValue(3);

  @Mock
  private AsyncRequestContext context;

  private final CachingFilter<String, Integer> cachingFilter = new CachingFilter<>(KEY_GENERATOR, 100, 200, TimeUnit.MILLISECONDS);

  @Before
  public void setup() {
    Mockito.when(context.getParams()).thenReturn(new Object[] {"ignore"});
    Mockito.when(context.invokeAsync()).thenReturn(VALUE1, VALUE2, VALUE3);
  }

  @Test
  public void testCaching() throws ExecutionException, InterruptedException {
    final int res1 = cachingFilter.handleAsync(context).get();
    final int res2 = cachingFilter.handleAsync(context).get();
    Assert.assertEquals(res1, res2);
    Assert.assertEquals(VALUE1.get(), res1);

    Thread.sleep(300);

    final int res3 = cachingFilter.handleAsync(context).get();

    Assert.assertNotEquals(res2, res3);
    Assert.assertEquals(VALUE2.get(), res3);
  }

}
