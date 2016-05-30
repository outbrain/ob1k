package com.outbrain.ob1k.client.dispatch;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.endpoints.AsyncClientEndpoint;
import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.endpoints.StreamClientEndpoint;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.atomic.AtomicInteger;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;
import static java.util.Collections.nCopies;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

/**
 * @author marenzon
 */
public class DispatchStrategyTest {

  private static final Object[] PARAMS = new Object[]{"one", "two"};
  private static final String FAKE_REMOTE = "http://my.service";
  private static final int DOUBLE_DISPATCH_DURATION = 50;

  @Test
  public void testDefaultDispatchStrategyForAsyncEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final AsyncClientEndpoint clientEndpoint = createAsyncClientEndpoint(invokeCounter);
    final DispatchAction dispatchAction = clientEndpoint.createDispatchAction(PARAMS);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy defaultDispatchStrategy = DefaultDispatchStrategy.INSTANCE;
    clientEndpoint.dispatch(targetProvider, defaultDispatchStrategy, dispatchAction);
    assertEquals("only one dispatch should occur", 1, invokeCounter.get());
  }

  @Test
  public void testDefaultDispatchStrategyForStreamEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final StreamClientEndpoint clientEndpoint = createStreamClientEndpoint(invokeCounter);
    final DispatchAction dispatchAction = clientEndpoint.createDispatchAction(PARAMS);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy defaultDispatchStrategy = DefaultDispatchStrategy.INSTANCE;
    clientEndpoint.dispatch(targetProvider, defaultDispatchStrategy, dispatchAction);
    assertEquals("only one dispatch should occur", 1, invokeCounter.get());
  }

  @Test
  public void testStaticDoubleDispatchStrategyForAsyncEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final AsyncClientEndpoint clientEndpoint = createAsyncClientEndpoint(invokeCounter);
    final DispatchAction dispatchAction = clientEndpoint.createDispatchAction(PARAMS);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy doubleDispatchStrategy = new StaticDoubleDispatchStrategy(DOUBLE_DISPATCH_DURATION);
    clientEndpoint.dispatch(targetProvider, doubleDispatchStrategy, dispatchAction);
    assertEquals("double dispatch should not occur", 1, invokeCounter.get());
  }

  @Test
  public void testStaticDoubleDispatchStrategyForStreamEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final StreamClientEndpoint clientEndpoint = createStreamClientEndpoint(invokeCounter);
    final DispatchAction dispatchAction = clientEndpoint.createDispatchAction(PARAMS);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy doubleDispatchStrategy = new StaticDoubleDispatchStrategy(DOUBLE_DISPATCH_DURATION);
    clientEndpoint.dispatch(targetProvider, doubleDispatchStrategy, dispatchAction);
    assertEquals("double dispatch should not occur", 1, invokeCounter.get());
  }

  @Test
  public void testStaticDoubleDispatchOccursForAsyncEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final DispatchAction dispatchAction = remoteTarget -> {
      invokeCounter.incrementAndGet();
      return schedule(() -> "hello world", DOUBLE_DISPATCH_DURATION * 2, MILLISECONDS);
    };
    final AsyncClientEndpoint clientEndpoint = createAsyncClientEndpoint(dispatchAction);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy doubleDispatchStrategy = new StaticDoubleDispatchStrategy(DOUBLE_DISPATCH_DURATION);
    final ComposableFuture dispatchFuture = (ComposableFuture) clientEndpoint.dispatch(targetProvider,
      doubleDispatchStrategy, dispatchAction);

    dispatchFuture.get(); // await for schedule of double dispatch to happen

    assertEquals("double dispatch should occur", 2, invokeCounter.get());
  }

  @Test
  public void testStaticDoubleDispatchNotOccursForStreamEndpoint() throws Exception {
    final AtomicInteger invokeCounter = new AtomicInteger();
    final DispatchAction dispatchAction = remoteTarget -> {
      invokeCounter.incrementAndGet();
      return just("hello world").delay(DOUBLE_DISPATCH_DURATION * 2, MILLISECONDS);
    };
    final StreamClientEndpoint clientEndpoint = createStreamClientEndpoint(dispatchAction);
    final TargetProvider targetProvider = createTargetProvider();

    final DispatchStrategy doubleDispatchStrategy = new StaticDoubleDispatchStrategy(DOUBLE_DISPATCH_DURATION);
    final Observable observable = (Observable) clientEndpoint.dispatch(targetProvider, doubleDispatchStrategy,
      dispatchAction);

    observable.toBlocking(); // await

    assertEquals("double dispatch should not occur", 1, invokeCounter.get());
  }

  private static AsyncClientEndpoint createAsyncClientEndpoint(final AtomicInteger invokeCounter) {
    return createAsyncClientEndpoint(remoteTarget -> {
      invokeCounter.incrementAndGet();
      return fromValue("hello world");
    });
  }

  private static AsyncClientEndpoint createAsyncClientEndpoint(final DispatchAction dispatchAction) {
    return createClientEndpoint(AsyncClientEndpoint.class, dispatchAction);
  }

  private static StreamClientEndpoint createStreamClientEndpoint(final AtomicInteger invokeCounter) {
    return createStreamClientEndpoint(remoteTarget -> {
      invokeCounter.incrementAndGet();
      return just("hello world");
    });
  }

  private static StreamClientEndpoint createStreamClientEndpoint(final DispatchAction dispatchAction) {
    return createClientEndpoint(StreamClientEndpoint.class, dispatchAction);
  }

  private static <T extends AbstractClientEndpoint> T createClientEndpoint(final Class<T> endpointType,
                                                                           final DispatchAction action) {
    final T clientMock = mock(endpointType);

    when(clientMock.createDispatchAction(PARAMS)).thenReturn(action);
    when(clientMock.dispatch(any(TargetProvider.class), any(DispatchStrategy.class),
      any(DispatchAction.class))).thenCallRealMethod();

    return clientMock;
  }

  private static TargetProvider createTargetProvider() {
    final TargetProvider targetProvider = mock(TargetProvider.class);

    when(targetProvider.provideTarget()).thenReturn(FAKE_REMOTE);
    when(targetProvider.provideTargets(any(Integer.class))).thenAnswer(invocation ->
      nCopies((int) invocation.getArguments()[0], FAKE_REMOTE));

    return targetProvider;
  }
}