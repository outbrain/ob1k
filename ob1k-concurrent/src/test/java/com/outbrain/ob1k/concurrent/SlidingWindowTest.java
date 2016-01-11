package com.outbrain.ob1k.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by aronen on 7/7/14.
 *
 * tests the sliding window.
 */
public class SlidingWindowTest {
  @Test
  public void testNonConcurrent() {
    final ConcurrentSlidingWindow<String> window = new ConcurrentSlidingWindow<>(3);
    window.offer("1");
    window.offer("2");

    List<String> results = new ArrayList<>();
    for (final String element: window) {
      results.add(element);
    }

    Assert.assertEquals(2, results.size());
    Assert.assertEquals("1", results.get(0));
    Assert.assertEquals("2", results.get(1));

    window.offer("3");
    window.offer("4");


    results = new ArrayList<>();
    for (final String element: window) {
      results.add(element);
    }

    Assert.assertEquals(3, results.size());
    Assert.assertEquals("2", results.get(0));
    Assert.assertEquals("3", results.get(1));
    Assert.assertEquals("4", results.get(2));
  }

  @Test
  public void testConcurrent() throws InterruptedException {
    final ConcurrentSlidingWindow<String> window = new ConcurrentSlidingWindow<>(3);

    final List<Updater> updaters = new ArrayList<>();
    final AtomicBoolean active = new AtomicBoolean(true);
    final int SIZE = 5;
    final CountDownLatch firstStep = new CountDownLatch(SIZE);
    for (int i=0; i< SIZE; i++) {
      final Updater updater = new Updater(window, active, firstStep);
      updaters.add(updater);
      updater.start();
    }

    int counter = 0;
    for (final String element : window) {
      counter++;
    }
    Assert.assertTrue("counter=" + counter, counter <= 3);
    firstStep.await();

    counter = 0;
    for (final String element : window) {
      counter++;
    }
    Assert.assertTrue("counter=" + counter, counter == 3);

    active.set(false);
    for(final Updater updater : updaters) {
      updater.join();
    }

    final List<String> results1 = new ArrayList<>();
    for (final String element : window) {
      results1.add(element);
    }

    final List<String> results2 = new ArrayList<>();
    for (final String element : window) {
      results2.add(element);
    }

    Assert.assertEquals(3, results1.size());
    Assert.assertEquals(3, results2.size());

    Assert.assertEquals(results1, results2);

  }

  private static class Updater extends Thread {
    private final ConcurrentSlidingWindow<String> window;
    private final AtomicBoolean active;
    private final CountDownLatch firstStep;

    private Updater(final ConcurrentSlidingWindow<String> window, final AtomicBoolean active, final CountDownLatch firstStep) {
      this.window = window;
      this.active = active;
      this.firstStep = firstStep;
    }

    @Override
    public void run() {
      int counter = 0;
      while (active.get()) {
        window.offer(UUID.randomUUID().toString());
        counter++;
        if (counter == 3) {
          firstStep.countDown();
        }
      }
    }
  }
}
