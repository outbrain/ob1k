package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.DoubleDispatchStrategy;
import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests for double dispatch implementation of AsyncClientEndpoint
 *
 * @author lifey
 */
public class AsyncClientEndpointTest {
  private static final long BASE_TIME_INTERVAL_MS = 100;

  @Test
  public void testNoDoubleDispatchStrategy() throws Throwable {
    final TestTargetProvider targetProvider = new TestTargetProvider("noStrategy",2) ;
    final AsyncClientEndpoint asyncClientEndpoint = createDummyAsyncClientEndpoint(null,BASE_TIME_INTERVAL_MS*2);
    final ComposableFuture<Integer> result = (ComposableFuture<Integer>) asyncClientEndpoint.invoke(targetProvider,null);
    assertEquals(1,result.get().intValue());
    assertEquals(1,targetProvider.getProvideTargetCount());
  }

  @Test
  public void testDoubleDispatchNotTriggered() throws Throwable {
    final TestDoubleDispatchStrategy doubleDispatchStrategy = new TestDoubleDispatchStrategy();
    final TestTargetProvider targetProvider = new TestTargetProvider("single",1) ;
    final AsyncClientEndpoint asyncClientEndpoint = createDummyAsyncClientEndpoint(doubleDispatchStrategy,BASE_TIME_INTERVAL_MS/2);
    final ComposableFuture<Integer> result = (ComposableFuture<Integer>) asyncClientEndpoint.invoke(targetProvider,null);
    assertEquals(1,result.get().intValue());
    assertEquals(1,targetProvider.getProvideTargetCount());
    assertEquals(1,doubleDispatchStrategy.getOnCompleteInvocations());
  }
  @Test
  public void testDoubleDispatchTriggered() throws Throwable {
    final TestDoubleDispatchStrategy doubleDispatchStrategy = new TestDoubleDispatchStrategy();
    final TestTargetProvider targetProvider = new TestTargetProvider("double",2) ;
    final AsyncClientEndpoint asyncClientEndpoint = createDummyAsyncClientEndpoint(doubleDispatchStrategy,BASE_TIME_INTERVAL_MS*2);
    final ComposableFuture<Integer> result = (ComposableFuture<Integer>) asyncClientEndpoint.invoke(targetProvider,null);
    assertEquals(2,result.get().intValue());
    assertEquals(2,targetProvider.getProvideTargetCount());
    assertEquals(1,doubleDispatchStrategy.getOnCompleteInvocations());
  }

  @Test
  public void testDoubleDispatchTriggeredWithProvideTargetRetry() throws Throwable {
    final TestDoubleDispatchStrategy doubleDispatchStrategy = new TestDoubleDispatchStrategy();
    final TestTargetProvider targetProvider = new TestTargetProvider("triple",1) ; // single target....
    final AsyncClientEndpoint asyncClientEndpoint = createDummyAsyncClientEndpoint(doubleDispatchStrategy,BASE_TIME_INTERVAL_MS*2);
    final ComposableFuture<Integer> result = (ComposableFuture<Integer>) asyncClientEndpoint.invoke(targetProvider,null);
    assertEquals(2,result.get().intValue());
    assertEquals(3,targetProvider.getProvideTargetCount());
    assertEquals(1,doubleDispatchStrategy.getOnCompleteInvocations());
  }

  // A provider which performs a round robing on dummy targets
  private static class TestTargetProvider implements TargetProvider {
    private final AtomicInteger provideTargetCount = new AtomicInteger();
    private final String prefix;
    private final long numTargets;

    public TestTargetProvider(final String prefix, final long numTargets) {
      this.prefix = prefix;
      this.numTargets = numTargets;
    }

    @Override
    public String getTargetLogicalName() {
      return "logically";
    }

    @Override
    public String provideTarget() {
      final String target = prefix + (provideTargetCount.incrementAndGet() % numTargets);
      return target;
    }

    public int getProvideTargetCount() {
      return provideTargetCount.get();
    }
  }

  private class TestDoubleDispatchStrategy implements DoubleDispatchStrategy {
    private final AtomicInteger onCompleteInvocations = new AtomicInteger();
    @Override
    public long getDoubleDispatchIntervalMs() {
      return BASE_TIME_INTERVAL_MS;
    }

    @Override
    public void onComplete(final Try result, final long startTimeMs) {
      onCompleteInvocations.incrementAndGet();
    }

    public int getOnCompleteInvocations() {
      return onCompleteInvocations.get();
    }
  }

  // this generated client can be used only once
  private AsyncClientEndpoint createDummyAsyncClientEndpoint(final DoubleDispatchStrategy doubleDispatchStrategy,final long opDelay) {
    final AbstractClientEndpoint.Endpoint endpoint = new AbstractClientEndpoint.Endpoint(null,null,null,"/dd",null);
    return new AsyncClientEndpoint(null,null,endpoint,null,doubleDispatchStrategy) {
      AtomicInteger invocations = new AtomicInteger();
      @Override
      public ComposableFuture<Integer> invokeAsync(final AsyncClientRequestContext ctx) {
        final int invocation = invocations.incrementAndGet();
        final long effectiveOpDelay = invocation == 1 ? opDelay : opDelay/20; //short delay on the second invocation of double dispatch
        return ComposableFutures.scheduleFuture(() -> ComposableFutures.fromValue(invocation), effectiveOpDelay , TimeUnit.MILLISECONDS);
      }
    };
  }
}