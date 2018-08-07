package com.outbrain.ob1k.concurrent;

import com.outbrain.ob1k.concurrent.combiners.BiFunction;
import com.outbrain.ob1k.concurrent.combiners.TriFunction;
import com.outbrain.ob1k.concurrent.eager.EagerComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureProvider;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.outbrain.ob1k.concurrent.ComposableFutures.all;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batch;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batchToStream;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batchUnordered;
import static com.outbrain.ob1k.concurrent.ComposableFutures.combine;
import static com.outbrain.ob1k.concurrent.ComposableFutures.first;
import static com.outbrain.ob1k.concurrent.ComposableFutures.flatten;
import static com.outbrain.ob1k.concurrent.ComposableFutures.foreach;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.recursive;
import static com.outbrain.ob1k.concurrent.ComposableFutures.repeat;
import static com.outbrain.ob1k.concurrent.ComposableFutures.retry;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;
import static com.outbrain.ob1k.concurrent.ComposableFutures.submit;
import static com.outbrain.ob1k.concurrent.ComposableFutures.toHotObservable;
import static com.outbrain.ob1k.concurrent.ComposableFutures.toObservable;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: aronen
 * Date: 7/2/13
 * Time: 10:20 AM
 */
public class ComposableFutureTest {

  private static final int ITERATIONS = 100000;

  private static long computeHash(final long seed) {
    long value = seed;
    for (int i = 0; i < 10000; i++) {
      value ^= value << 13;
      value ^= value >>> 17;
      value ^= value << 5;
    }

    return value;
  }

  @Test
  public void testForeach() throws ExecutionException, InterruptedException {
    final List<Integer> numbers = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      numbers.add(i);
    }

    final List<Integer> empty = new ArrayList<>();
    final ComposableFuture<List<Integer>> first3Even = foreach(numbers, empty, (element, aggregateResult) -> {
      if (aggregateResult.size() < 3 && element % 2 == 0) {
        aggregateResult.add(element);
      }

      return fromValue(aggregateResult);
    });

    final List<Integer> result = first3Even.get();
    assertEquals(result.size(), 3);
    assertEquals(result.get(0), new Integer(2));
    assertEquals(result.get(1), new Integer(4));
    assertEquals(result.get(2), new Integer(6));
  }

  @Test
  public void testRepeat() throws Exception {
    final ComposableFuture<Integer> future = repeat(10, 0, result -> fromValue(result + 1));
    assertEquals(10, (int) future.get());
  }

  @Test
  public void testEnsure() throws Exception {
    final ComposableFuture<String> future = fromValue("hello").ensure(String::isEmpty);
    try {
      future.get();
    } catch (final ExecutionException e) {
      assertEquals("ensure failed the chain with NoSuchElementException",
        NoSuchElementException.class, e.getCause().getClass());
      return;
    }

    fail("exception should have been thrown");
  }

  @Test
  public void testAndThen() throws Exception {
    final List<String> results = new ArrayList<>(1);
    final ComposableFuture<String> future = fromValue("hello").
      andThen(valueTry -> results.add(valueTry.getValue()));

    final String value = future.get();

    assertEquals("future value should be still 'hello'", "hello", value);
    assertEquals("list should contain one result", 1, results.size());
  }

  @Test
  public void testPeekSucess() throws Exception {
    final List<String> results = new ArrayList<>();
    final ComposableFuture<String> future = fromValue("hello").peek(results::add);

    final String value = future.get();

    assertEquals("future value should be still 'hello'", "hello", value);
    assertEquals("list should contain one result", 1, results.size());
  }

  @Test
  public void testPeekFailure() {
    final List<String> results = new ArrayList<>();
    final ComposableFuture<String> future = ComposableFutures.<String>fromError(new Exception()).peek(results::add);

    try {
      future.get();
      fail("should have failed");
    } catch (final Exception e) {
      assertTrue("list should not be modified", results.isEmpty());
    }
  }

  @Test
  public void testSuccessful() throws Exception {
    final ComposableFuture<String> failureFuture = fromError(new RuntimeException("failureFuture"));
    final ComposableFuture<Try<String>> successfulFuture = failureFuture.successful();

    final Try<String> failureTry = successfulFuture.get();
    assertTrue("Try is Failure", failureTry.isFailure());
    assertEquals("Failure type is RuntimeException", RuntimeException.class, failureTry.getError().getClass());
  }

  @Test
  public void testDelayedFuture() throws Exception {
    final ComposableFuture<Long> successFuture = fromValue(currentTimeMillis());
    final long timeTook = successFuture.delay(1000, MILLISECONDS).map(time -> currentTimeMillis() - time).get();

    assertTrue("thread should have been waiting for at least 500ms (1s delay)", timeTook > 500);
  }

  @Test
  public void testFlattenSuccess() throws Exception {
    final ComposableFuture<String> successFuture = fromValue("deep");
    final ComposableFuture<ComposableFuture<String>> successNestedFuture = fromValue(successFuture);
    final ComposableFuture<String> successFlatten = flatten(successNestedFuture);

    assertEquals("flatten and original should have same value", successFuture.get(), successFlatten.get());
  }

  @Test
  public void testFlattenFailure() throws Exception {
    final ComposableFuture<String> errorFuture = fromError(new IllegalStateException("errr"));
    final ComposableFuture<ComposableFuture<String>> errorNestedFuture = fromValue(errorFuture);
    final ComposableFuture<String> errorFlatten = flatten(errorNestedFuture);

    Throwable cause = getFutureCause(errorFuture);
    Throwable causeFlatten = getFutureCause(errorFlatten);
    assertEquals("flatten and original should have same value", cause, causeFlatten);
  }

  private Throwable getFutureCause(ComposableFuture<String> errorFuture) {
    try {
      errorFuture.get();
    } catch (Exception e) {
      return e.getCause();
    }
    throw new IllegalArgumentException("no future cause");
  }

  @Test
  public void testRecursive() throws Exception {
    final AtomicInteger atomicInteger = new AtomicInteger();
    final ComposableFuture<Integer> future = recursive(() -> fromValue(atomicInteger.incrementAndGet()), input -> input >= 10);
    assertEquals(10, (int) future.get());
  }

  @Test
  public void testBatch() throws Exception {
    final List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final ComposableFuture<List<String>> res = batch(nums, 2, result -> schedule(() -> "num:" + result, 1, TimeUnit.SECONDS));

    final List<String> results = res.get();
    assertEquals(results.size(), nums.size());

  }

  @Test
  public void testRetry() throws Exception {
    final int retries = 1;

    final ComposableFuture<Integer> retryOperation = retry(retries, (attempt) -> {
      if (attempt < retries) {
        return fromError(new RuntimeException());
      }

      return fromValue(attempt);
    });

    final int result = retryOperation.get();
    assertEquals("successful attempt should be last one", retries, result);
  }

  @Test
  public void testRetryCatchesUnhandledException() throws Throwable {
    final String exceptionMessage = "very bad exception";
    final AtomicInteger attempts = new AtomicInteger(0);
    final int retries = 5;

    final ComposableFuture<String> failedOperation = retry(retries, (attempt) -> {
      attempts.incrementAndGet();
      throw new RuntimeException(exceptionMessage);
    });

    // error unboxing
    final String resultedExceptionMessage = Try.apply(failedOperation::get).
      recoverWith(ExecutionException.class, error -> Try.fromError(error.getCause())).
      recover(RuntimeException.class, Throwable::getMessage).
      getValue();

    assertEquals("attempts number should be equal to retries plus first run", retries + 1, attempts.get());
    assertEquals("resulted error is our unhandled exception", exceptionMessage, resultedExceptionMessage);
  }

  @Test
  public void testBatchUnorderedAtMostParallelism() throws Exception {
    testBatchUnordered((elements, parallelism) -> {
      final CyclicBarrier barrier = new CyclicBarrier(parallelism + 1);
      final CyclicBarrier resettingBarrier = new CyclicBarrier(parallelism, barrier::reset);
      final AtomicInteger processed = new AtomicInteger();
      final int end = elements.size() / parallelism * parallelism;

      return batchUnordered(elements, parallelism, produce(num -> {
        try {
          barrier.await(100, TimeUnit.MICROSECONDS);
          fail("parallelism exceeded " + parallelism);
        } catch (TimeoutException | BrokenBarrierException e) {
          if (processed.getAndIncrement() < end) {
            resettingBarrier.await();
          }
        }
      }));
    });
  }

  @Test
  public void testBatchUnorderedAtLeastParallelism() throws Exception {
    testBatchUnordered((elements, parallelism) -> {
      final CyclicBarrier barrier = new CyclicBarrier(parallelism);
      final AtomicInteger processed = new AtomicInteger();
      final int end = elements.size() / parallelism * parallelism;
      return batchUnordered(elements, parallelism, produce(num -> {
        if (processed.getAndIncrement() < end) {
          barrier.await(1, TimeUnit.SECONDS);
        }
      }));
    });
  }

  @Test
  public void testBatchUnorderedDoesNotWaitForSlowlyProcessedElements() throws Exception {
    testBatchUnordered((elements, parallelism) -> {
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicInteger processed = new AtomicInteger();
      final int elementToGetStuckAt = ThreadLocalRandom.current().nextInt(elements.size());
      return batchUnordered(elements, parallelism, produce(num -> {
        if (parallelism > 1 && num == elementToGetStuckAt) {
          assertTrue(latch.await(1, TimeUnit.SECONDS));
        }
        if (processed.incrementAndGet() == elements.size() - 1) {
          latch.countDown();
        }
      }));
    });
  }

  @Test
  public void testBatchUnorderedDoesNotCauseStackOverflowErrorSync() throws Exception {
    batchUnordered(Collections.nCopies(100_000, 1), 1, result -> ComposableFutures.fromNull()).get();
  }

  @Test
  public void testBatchUnorderedDoesNotCauseStackOverflowErrorAsync() throws Exception {
    batchUnordered(Collections.nCopies(100_000, 1), 2, result -> ComposableFutures.submit(() -> 0)).get();
  }

  private interface CheckedConsumer<T> {
    void accept(T element) throws Exception;
  }

  private FutureSuccessHandler<Integer, Integer> produce(final CheckedConsumer<Integer> r) {
    return num -> ComposableFutures.submit(() -> {
      r.accept(num);
      return 2 * num;
    });
  }

  private void testBatchUnordered(final BiFunction<List<Integer>, Integer, ComposableFuture<List<Integer>>> test) throws Exception {
    final List<Integer> nums = IntStream.range(0, 1000).boxed().collect(toList());
    for (int parallelism = 1; parallelism < 10; parallelism++) {
      final List<Integer> results = test.apply(nums, parallelism).get();
      assertEquals(nums.stream().map(x -> 2 * x).collect(toList()), results.stream().sorted().collect(toList()));
    }
  }

  @Test
  public void testBatchToStream() throws Exception {
    final List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final Observable<List<String>> stream = batchToStream(nums, 2, result -> schedule(() -> "num:" + result, 1, TimeUnit.SECONDS));

    final Iterable<List<String>> iterator = stream.toBlocking().toIterable();
    int totalElements = 0;
    for (final List<String> batch : iterator) {
      final int batchSize = batch.size();
      totalElements += batchSize;
      assertEquals(batchSize, 2);
    }
    assertEquals(totalElements, nums.size());
  }

  @Test
  @Ignore("performance test")
  public void testThreadPool() {
    testWithRegularThreadPool(true);
    //    testWithRegularThreadPool(false);
  }

  @Test
  @Ignore("performance test")
  public void testSingleThreadBenchmark() {
    final long t1 = currentTimeMillis();
    long sum = 0;

    for (long i = 0; i < ITERATIONS; i++) {
      final long phase1 = computeHash(i);
      final long phase2 = computeHash(phase1);
      final long phase3 = computeHash(phase2);
      sum += phase3;
    }

    final long t2 = currentTimeMillis();
    System.out.println("total time: " + (t2 - t1) + " for sum: " + sum);
  }

  private void testWithRegularThreadPool(final boolean delegate) {
    final List<ComposableFuture<Long>> futures = new ArrayList<>();

    for (int i = 0; i < ITERATIONS; i++) {
      final long seed = i;
      final ComposableFuture<Long> f1 = submit(delegate, () -> computeHash(seed));
      final ComposableFuture<Long> f2 = f1.map(ComposableFutureTest::computeHash);
      final ComposableFuture<Long> f3 = f2.map(ComposableFutureTest::computeHash);

      futures.add(f3);
    }

    final ComposableFuture<List<Long>> all = all(futures);
    try {
      final List<Long> res = all.get();
      long sum = 0;
      for (final long num : res) {
        sum += num;
      }
    } catch (final Exception ignored) {
    }
  }

  @Test
  public void testContinuations() {
    final ComposableFuture<String> res =
      schedule(() -> "lala", 100, MILLISECONDS).alwaysWith(result -> fromError(new RuntimeException("bhaaaaa")))
        .map(result -> "second lala")
        .recover(error -> "third lala")
        .recover(error -> "baaaaddddd")
        .map(result -> {
          throw new UncheckedExecutionException(new RuntimeException("booo"));
        });

    try {
      res.get();
      fail("got result instead of an exception");
    } catch (InterruptedException | ExecutionException e) {
      final String exTypeName = e.getCause().getClass().getName();
      assertEquals(exTypeName, RuntimeException.class.getName());
    }
  }

  @Test
  public void testComposingFutureTypes() {
    final String name = "haim";
    final int age = 23;
    final double weight = 70.3;

    final ComposableFuture<String> futureName = fromValue(name);
    final ComposableFuture<Integer> futureAge = fromValue(age);
    final ComposableFuture<Double> futureWeight = fromValue(weight);

//      final ComposableFuture<Double> weight = fromError(new RuntimeException("Illegal Weight error!"));

    final ComposableFuture<Person> person = combine(futureName, futureAge, futureWeight,
      (TriFunction<String, Integer, Double, Person>) (name1, age1, weight1) -> new Person(age1, name1, weight1));

    try {
      final Person result = person.get();
      assertEquals(result.age, age);
      assertEquals(result.name, name);
      assertEquals(result.weight, weight, 0);
    } catch (InterruptedException | ExecutionException e) {
      fail(e.getMessage());
    }

    final ComposableFuture<String> first = fromValue("1");
    final ComposableFuture<Integer> second = fromValue(2);
    final ComposableFuture<Object> badRes = combine(first, second,
      (BiFunction<String, Integer, Object>) (left, right) -> {
        throw new ExecutionException(new RuntimeException("not the same..."));
      });

    try {
      badRes.get();
      fail("should get an error");
    } catch (final InterruptedException e) {
      fail(e.getMessage());
    } catch (final ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("not the same..."));
    }

  }

  @Test
  public void testCatchingThrowable() throws Exception {
    final ComposableFuture<Object> failedFuture = fromValue("Success").map(__ -> {
      throw new OutOfMemoryError();
    });

    final AtomicReference<Boolean> failed = new AtomicReference<>(false);
    final CountDownLatch latch = new CountDownLatch(1);
    failedFuture.consume(result -> {
      failed.set(result.isFailure());
      latch.countDown();
    });

    latch.await();

    assertTrue("consume should be called, with try of OutOfMemoryError", failed.get());
  }

  @Test
  public void testSlowFuture() {
    final ComposableFuture<String> f1 = schedule(() -> "slow", 1, TimeUnit.SECONDS);

    final ComposableFuture<String> f2 = fromValue("fast1");
    final ComposableFuture<String> f3 = fromValue("fast2");

    final ComposableFuture<List<String>> res = all(Arrays.asList(f1, f2, f3));
    final long t1 = currentTimeMillis();
    try {
      res.get();
      final long t2 = currentTimeMillis();
      assertTrue("time is: " + (t2 - t1), (t2 - t1) > 900); // not
    } catch (InterruptedException | ExecutionException e) {
      fail(e.getMessage());
    }

    final ComposableFuture<String> f4 = schedule(() -> "slow", 1, TimeUnit.SECONDS);
    final ComposableFuture<String> f5 = fromError(new RuntimeException("oops"));
    final ComposableFuture<List<String>> res2 = all(true, Arrays.asList(f4, f5));
    final long t3 = currentTimeMillis();
    try {
      res2.get();
      fail("should get error.");
    } catch (InterruptedException | ExecutionException e) {
      final long t4 = currentTimeMillis();
      assertTrue((t4 - t3) < 100);
    }

    final ComposableFuture<String> f6 = schedule(() -> "slow", 1, TimeUnit.SECONDS);
    final ComposableFuture<String> f7 = fromError(new RuntimeException("oops"));
    final ComposableFuture<List<String>> res3 = all(true, Arrays.asList(f6, f7));
    final long t5 = currentTimeMillis();
    try {
      res3.get();
      fail("should get error.");
    } catch (InterruptedException | ExecutionException e) {
      final long t6 = currentTimeMillis();
      System.out.println("time took to fail: " + (t6 - t5));
      assertTrue((t6 - t5) < 100);
    }

  }

  @SuppressWarnings("Convert2MethodRef")
  @Test
  public void testFuturesToStream() throws InterruptedException {
    final ComposableFuture<Long> first = schedule(() -> currentTimeMillis(), 1, TimeUnit.SECONDS);

    final ComposableFuture<Long> second = schedule(() -> currentTimeMillis(), 2, TimeUnit.SECONDS);

    final ComposableFuture<Long> third = schedule(() -> currentTimeMillis(), 3, TimeUnit.SECONDS);

    final Iterable<Long> events = toHotObservable(Arrays.asList(first, second, third), true).toBlocking().toIterable();
    long prevEvent = 0;
    int counter = 0;
    for (final Long event : events) {
      counter++;
      assertTrue("event should have bigger timestamp than the previous one", event > prevEvent);
      prevEvent = event;
    }

    assertEquals("should receive 3 events", counter, 3);

  }

  @Test
  public void testFutureProviderToStream() {
    final Observable<Long> stream = toObservable(new FutureProvider<Long>() {
      private volatile int index = 3;
      private volatile ComposableFuture<Long> currentRes;

      @SuppressWarnings("Convert2MethodRef")
      @Override
      public boolean moveNext() {
        if (index > 0) {
          index--;
          currentRes = schedule(() -> currentTimeMillis(), 100, MILLISECONDS);

          return true;
        } else {
          return false;
        }
      }

      @Override
      public ComposableFuture<Long> current() {
        return currentRes;
      }
    });

    long current = currentTimeMillis();
    final Iterable<Long> events = stream.toBlocking().toIterable();
    int counter = 0;
    for (final Long event : events) {
      assertTrue(event > current);
      current = event;
      counter++;
    }

    assertTrue(counter == 3);

  }

  @Test
  public void testFirstNoTimeout() throws Exception {
    final PassThroughCount passThroughCount = new PassThroughCount(3);
    try {
      final Map<String, ComposableFuture<String>> elements = createElementsMap(passThroughCount);

      final Map<String, String> res = first(elements, 3).get();
      assertEquals(3, res.size());
      assertEquals("one", res.get("one"));
      assertEquals("two", res.get("two"));
      assertEquals("three", res.get("three"));
    } finally {
      passThroughCount.releaseAllWaiters(); // release last two guys
    }
  }

  @Test
  public void testFirstWithTimeout() throws Exception {
    final PassThroughCount passThroughCount = new PassThroughCount(2);
    try {
      final Map<String, ComposableFuture<String>> elements = createElementsMap(passThroughCount);
      passThroughCount.waitForPassers();  // we do not want that the first two elements will not finish due to scheduling issues
      final Map<String, String> res = first(elements, 3, 10, MILLISECONDS).get();

      assertEquals(2, res.size());
      assertEquals("one", res.get("one"));
      assertEquals("two", res.get("two"));
    } finally {
      passThroughCount.releaseAllWaiters(); // release last two guys
    }
  }

  @Test
  public void testAllFailOnError() throws Exception {
    final PassThroughCount passThroughCount = new PassThroughCount(5);
    final Map<String, ComposableFuture<String>> elements = createElementsMap(passThroughCount);

    try {
      all(true, elements).get();
      fail("should get an exception");
    } catch (final ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("bad element"));
    } finally {
      passThroughCount.releaseAllWaiters(); // release last two guys
    }

  }

  @Test
  public void testAllFailFast() throws Exception {
    final Map<String, ComposableFuture<String>> elements = new HashMap<>();

    elements.put("one", submit(() -> {
      Thread.sleep(100);
      return "one";
    }));

    elements.put("two", submit(() -> {
      throw new RuntimeException("error...");
    }));

    final long t1 = currentTimeMillis();
    try {
      all(true, elements).get();
      fail("should fail");
    } catch (final ExecutionException e) {
      final long t2 = currentTimeMillis();
      assertTrue("should fail fast", (t2 - t1) < 50);
    }
  }

  private Map<String, ComposableFuture<String>> createElementsMap(final PassThroughCount passThroughCount) {
    final Map<String, ComposableFuture<String>> elements = new HashMap<>();

    elements.put("one", submit(() -> {
      passThroughCount.awaitOrPass(1);
      return "one";
    }));
    elements.put("two", submit(() -> {
      passThroughCount.awaitOrPass(2);
      return "two";
    }));
    elements.put("three", submit(() -> {
      passThroughCount.awaitOrPass(3);
      return "three";
    }));
    elements.put("four", submit(() -> {
      passThroughCount.awaitOrPass(4);
      throw new RuntimeException("bad element");
    }));
    elements.put("five", submit(() -> {
      passThroughCount.awaitOrPass(5);
      return "five";
    }));
    return elements;
  }

  @Test
  public void testTypedErrors() throws Exception {
    String result = ComposableFutures.<String>submit(() -> {
      throw new FileNotFoundException("no file...");
    }).recover(NullPointerException.class, error -> {
      return "failure";
    }).recover(FileNotFoundException.class, error -> {
      return "success";
    }).recover(error -> {
      return "failure";
    }).get();

    Assert.assertEquals("success", result);

    String result2 = ComposableFutures.<String>submit(() -> {
      throw new FileNotFoundException("no file...");
    }).recoverWith(NullPointerException.class, error -> {
      return ComposableFutures.fromValue("failure");
    }).recoverWith(FileNotFoundException.class, error -> {
      return ComposableFutures.fromValue("success");
    }).recoverWith(error -> {
      return ComposableFutures.fromValue("failure");
    }).get();

    Assert.assertEquals("success", result2);

  }

  @Test
  public void testWithTimeout() throws Exception {
    final String RES_STR = "result";
    final EagerComposableFuture<String> value = new EagerComposableFuture<>();

    final ComposableFuture<String> effectiveValue = value.withTimeout(100, MILLISECONDS);
    Thread.sleep(50);
    value.set(RES_STR);
    assertEquals(RES_STR, value.get());
    assertEquals(RES_STR, effectiveValue.get());

  }

  @Test(expected = ExecutionException.class)
  public void testWithTimeoutExpired() throws Exception {
    final String RES_STR = "result";
    final EagerComposableFuture<String> value = new EagerComposableFuture<>();

    final ComposableFuture<String> effectiveValue = value.withTimeout(50, MILLISECONDS);
    Thread.sleep(100);
    value.set(RES_STR);
    assertEquals(RES_STR, value.get());
    effectiveValue.get(); // this should throw an exception
  }

  private static final class Person {
    public final int age;
    public final String name;
    public final double weight;


    private Person(final int age, final String name, final double weight) {
      this.age = age;
      this.name = name;
      this.weight = weight;
    }
  }

  class PassThroughCount {
    final CountDownLatch waitersLatch;
    final CountDownLatch passersLatch;
    final int numToPass;

    public PassThroughCount(final int numToPass) {
      waitersLatch = new CountDownLatch(1);
      passersLatch = new CountDownLatch(numToPass);
      this.numToPass = numToPass;
    }

    public void awaitOrPass(final long myOrder) throws InterruptedException {
      if (myOrder > numToPass) waitersLatch.await();
      else passersLatch.countDown();
    }

    public void releaseAllWaiters() {
      waitersLatch.countDown();
    }

    public void waitForPassers() throws InterruptedException {
      passersLatch.await();
    }
  }

}
