package com.outbrain.ob1k.concurrent;

import com.google.common.collect.ImmutableSet;
import com.outbrain.ob1k.concurrent.combiners.BiFunction;
import com.outbrain.ob1k.concurrent.combiners.TriFunction;
import com.outbrain.ob1k.concurrent.eager.EagerComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureProvider;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.outbrain.ob1k.concurrent.ComposableFutures.all;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batch;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batchToStream;
import static com.outbrain.ob1k.concurrent.ComposableFutures.batchUnordered;
import static com.outbrain.ob1k.concurrent.ComposableFutures.combine;
import static com.outbrain.ob1k.concurrent.ComposableFutures.first;
import static com.outbrain.ob1k.concurrent.ComposableFutures.foreach;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.recursive;
import static com.outbrain.ob1k.concurrent.ComposableFutures.repeat;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;
import static com.outbrain.ob1k.concurrent.ComposableFutures.submit;
import static com.outbrain.ob1k.concurrent.ComposableFutures.toHotObservable;
import static com.outbrain.ob1k.concurrent.ComposableFutures.toObservable;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;

/**
 * User: aronen
 * Date: 7/2/13
 * Time: 10:20 AM
 */
public class ComposableFutureTest {

  public static final int ITERATIONS = 100000;

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
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get(0), new Integer(2));
    Assert.assertEquals(result.get(1), new Integer(4));
    Assert.assertEquals(result.get(2), new Integer(6));
  }

  @Test
  public void testRepeat() throws Exception {
    final ComposableFuture<Integer> future = repeat(10, 0, result -> fromValue(result + 1));
    Assert.assertEquals(10, (int) future.get());
  }

  @Test
  public void testEnsure() throws Exception {
    final ComposableFuture<String> future = fromValue("hello").ensure(String::isEmpty);
    try {
      future.get();
    } catch (final ExecutionException e) {
      Assert.assertEquals("ensure failed the chain with NoSuchElementException",
        NoSuchElementException.class, e.getCause().getClass());
      return;
    }

    Assert.fail("exception should have been thrown");
  }

  @Test
  public void testAndThen() throws Exception {
    final List<String> results = new ArrayList<>(1);
    final ComposableFuture<String> future = fromValue("hello").
      andThen(valueTry -> results.add(valueTry.getValue()));

    final String value = future.get();

    Assert.assertEquals("future value should be still 'hello'", "hello", value);
    Assert.assertEquals("list should contain one result", 1, results.size());
  }

  @Test
  public void testSuccessful() throws Exception {
    final ComposableFuture<String> failureFuture = fromError(new RuntimeException("failureFuture"));
    final ComposableFuture<Try<String>> successfulFuture = failureFuture.successful();

    final Try<String> failureTry = successfulFuture.get();
    Assert.assertTrue("Try is Failure", failureTry.isFailure());
    Assert.assertEquals("Failure type is RuntimeException", RuntimeException.class, failureTry.getError().getClass());
  }

  @Test
  public void testDelayedFuture() throws Exception {
    final ComposableFuture<Long> successFuture = fromValue(currentTimeMillis());
    final long timeTook = successFuture.delay(1000, MILLISECONDS).map(time -> currentTimeMillis() - time).get();

    Assert.assertTrue("thread should have been waiting for at least 500ms (1s delay)", timeTook > 500);
  }

  @Test
  public void testRecursive() throws Exception {
    final AtomicInteger atomicInteger = new AtomicInteger();
    final ComposableFuture<Integer> future = recursive(() -> fromValue(atomicInteger.incrementAndGet()), input -> input >= 10);
    Assert.assertEquals(10, (int) future.get());
  }

  @Test
  public void testBatch() throws Exception {
    final List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final ComposableFuture<List<String>> res = batch(nums, 2, result -> schedule(() -> "num:" + result, 1, TimeUnit.SECONDS));

    final List<String> results = res.get();
    Assert.assertEquals(results.size(), nums.size());

  }

  @Test
  public void testBatchUnordered() throws Exception {
    final List<Integer> nums = IntStream.range(1, 100_000).boxed().collect(toList());
    final ComposableFuture<List<Integer>> res = batchUnordered(nums, 8, num -> ComposableFutures.schedule(() -> num, 10, TimeUnit.MICROSECONDS));
    final List<Integer> results = res.get();
    nums.removeAll(ImmutableSet.copyOf(results));
    assertTrue(nums.isEmpty());
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
      Assert.assertEquals(batchSize, 2);
    }
    Assert.assertEquals(totalElements, nums.size());
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
      Assert.fail("got result instead of an exception");
    } catch (InterruptedException | ExecutionException e) {
      final String exTypeName = e.getCause().getClass().getName();
      Assert.assertEquals(exTypeName, RuntimeException.class.getName());
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

    final ComposableFuture<Person> person = combine(futureName, futureAge, futureWeight, new TriFunction<String, Integer, Double, Person>() {
      @Override
      public Person apply(final String name, final Integer age, final Double weight) {
        return new Person(age, name, weight);
      }
    });

    try {
      final Person result = person.get();
      Assert.assertEquals(result.age, age);
      Assert.assertEquals(result.name, name);
      Assert.assertEquals(result.weight, weight, 0);
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }

    final ComposableFuture<String> first = fromValue("1");
    final ComposableFuture<Integer> second = fromValue(2);
    final ComposableFuture<Object> badRes = combine(first, second, new BiFunction<String, Integer, Object>() {
      @Override
      public Object apply(final String left, final Integer right) throws ExecutionException {
        throw new ExecutionException(new RuntimeException("not the same..."));
      }
    });

    try {
      badRes.get();
      Assert.fail("should get an error");
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause().getMessage().contains("not the same..."));
    }

  }

  @Test
  public void testSlowFuture() {
    final ComposableFuture<String> f1 = schedule(() -> "slow", 1, TimeUnit.SECONDS);

    final ComposableFuture<String> f2 = fromValue("fast1");
    final ComposableFuture<String> f3 = fromValue("fast2");

    final ComposableFuture<List<String>> res = all(Arrays.asList(f1, f2, f3));
    final long t1 = currentTimeMillis();
    try {
      final List<String> results = res.get();
      final long t2 = currentTimeMillis();
      Assert.assertTrue("time is: " + (t2 - t1), (t2 - t1) > 900); // not
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }

    final ComposableFuture<String> f4 = schedule(() -> "slow", 1, TimeUnit.SECONDS);
    final ComposableFuture<String> f5 = fromError(new RuntimeException("oops"));
    final ComposableFuture<List<String>> res2 = all(true, Arrays.asList(f4, f5));
    final long t3 = currentTimeMillis();
    try {
      final List<String> results = res2.get();
      Assert.fail("should get error.");
    } catch (InterruptedException | ExecutionException e) {
      final long t4 = currentTimeMillis();
      Assert.assertTrue((t4 - t3) < 100);
    }

    final ComposableFuture<String> f6 = schedule(() -> "slow", 1, TimeUnit.SECONDS);
    final ComposableFuture<String> f7 = fromError(new RuntimeException("oops"));
    final ComposableFuture<List<String>> res3 = all(true, Arrays.asList(f6, f7));
    final long t5 = currentTimeMillis();
    try {
      final List<String> results = res3.get();
      Assert.fail("should get error.");
    } catch (InterruptedException | ExecutionException e) {
      final long t6 = currentTimeMillis();
      System.out.println("time took to fail: " + (t6 - t5));
      Assert.assertTrue((t6 - t5) < 100);
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
      Assert.assertTrue("event should have bigger timestamp than the previous one", event > prevEvent);
      prevEvent = event;
    }

    Assert.assertEquals("should receive 3 events", counter, 3);

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
      Assert.assertTrue(event > current);
      current = event;
      counter++;
    }

    Assert.assertTrue(counter == 3);

  }

  @Test
  public void testFirstNoTimeout() throws Exception {
    final PassThroughCount passThroughCount = new PassThroughCount(3);
    try {
      final Map<String, ComposableFuture<String>> elements = createElementsMap(passThroughCount);

      final Map<String, String> res = first(elements, 3).get();
      Assert.assertEquals(3, res.size());
      Assert.assertEquals("one", res.get("one"));
      Assert.assertEquals("two", res.get("two"));
      Assert.assertEquals("three", res.get("three"));
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

      Assert.assertEquals(2, res.size());
      Assert.assertEquals("one", res.get("one"));
      Assert.assertEquals("two", res.get("two"));
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
      Assert.fail("should get an exception");
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause().getMessage().contains("bad element"));
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
      Assert.fail("should fail");
    } catch (final ExecutionException e) {
      final long t2 = currentTimeMillis();
      Assert.assertTrue("should fail fast", (t2 - t1) < 50);
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
  public void testWithTimeout() throws Exception {
    final String RES_STR = "result";
    final EagerComposableFuture<String> value = new EagerComposableFuture<>();

    final ComposableFuture<String> effectiveValue = value.withTimeout(100, MILLISECONDS);
    Thread.sleep(50);
    value.set(RES_STR);
    Assert.assertEquals(RES_STR, value.get());
    Assert.assertEquals(RES_STR, effectiveValue.get());

  }

  @Test(expected = ExecutionException.class)
  public void testWithTimeoutExpired() throws Exception {
    final String RES_STR = "result";
    final EagerComposableFuture<String> value = new EagerComposableFuture<>();

    final ComposableFuture<String> effectiveValue = value.withTimeout(50, MILLISECONDS);
    Thread.sleep(100);
    value.set(RES_STR);
    Assert.assertEquals(RES_STR, value.get());
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
