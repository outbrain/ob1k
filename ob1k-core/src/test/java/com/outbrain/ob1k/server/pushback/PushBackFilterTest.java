package com.outbrain.ob1k.server.pushback;

import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.SyncRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PushBackFilterTest {

  public static final PushBackException PUSH_BACK = new PushBackException("pushBack");
  public static final String RETURN_VALUE = "VALUE";

  private PushBackFilter<String> filter;

  @Mock private PushBackStrategy strategy;
  @Mock private AsyncRequestContext asnycCtx;
  @Mock private SyncRequestContext syncCtx;
  @Mock(answer = Answers.RETURNS_MOCKS) private MetricFactory metricFactory;
  private  Try<String> result;

  @Before
  public void setup() throws ExecutionException {
    filter = new PushBackFilter<>(strategy, "", metricFactory);
    when(asnycCtx.invokeAsync()).thenReturn(ComposableFutures.fromValue(RETURN_VALUE));
    when(syncCtx.invokeSync()).thenReturn(RETURN_VALUE);
  }

  @Test
  public void shouldPushBackInAsyncCall() {
    // given
    when(strategy.allowRequest()).thenReturn(false);
    when(strategy.generateExceptionOnPushBack()).thenReturn(PUSH_BACK);

    //when
    filter.handleAsync(asnycCtx).consume(t -> result = t);

    // then
    verify(asnycCtx, never()).invokeAsync();
    verify(strategy, times(1)).done(false);
    assertSame(PUSH_BACK, result.getError());
  }

  @Test(expected = PushBackException.class)
  public void shouldPushBackInSyncCall() throws ExecutionException {
    // given
    when(strategy.allowRequest()).thenReturn(false);
    when(strategy.generateExceptionOnPushBack()).thenReturn(PUSH_BACK);

    //when
    filter.handleSync(syncCtx);

    // then
    verify(syncCtx, never()).invokeSync();
    verify(strategy, times(1)).done(false);
  }

  @Test
  public void shouldAllowThroughInAsyncCall() {
    // given
    when(strategy.allowRequest()).thenReturn(true);

    //when
    filter.handleAsync(asnycCtx).consume(t -> result = t);

    // then
    verify(asnycCtx, times(1)).invokeAsync();
    verify(strategy, times(1)).done(true);
    assertEquals(RETURN_VALUE, result.getValue());
  }

  @Test
  public void shouldAllowThroughInSyncCall() throws ExecutionException {
    // given
    when(strategy.allowRequest()).thenReturn(true);

    //when
    final String value = filter.handleSync(syncCtx);

    // then
    verify(syncCtx, times(1)).invokeSync();
    verify(strategy, times(1)).done(true);
    assertEquals(RETURN_VALUE, value);
  }
}