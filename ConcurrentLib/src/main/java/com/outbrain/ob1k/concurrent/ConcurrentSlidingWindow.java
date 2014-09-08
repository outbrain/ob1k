package com.outbrain.ob1k.concurrent;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by aronen on 7/7/14.
 *
 * creates a queue of finite size that holds the last N elements.
 * every offered element is added to the end of the queue.
 * in case of a full queue an element is removed from the head of the queue.
 *
 * getting the last N elements is done via the iterator.
 */
public class ConcurrentSlidingWindow<T> implements Iterable<T> {
  private final AtomicInteger size;
  private final Deque<T> window;
  private final int maxSize;

  public ConcurrentSlidingWindow(final int size) {
    this.maxSize = size;
    this.size = new AtomicInteger();
    this.window = new ConcurrentLinkedDeque<>();
  }

  public void offer(final T element) {
    final int newSIze = size.incrementAndGet();
    window.offerLast(element);
    if (newSIze > maxSize) {
      window.pollFirst();
      size.decrementAndGet();
    }
  }

  public Iterator<T> iterator() {
    final Iterator<T> internal = window.iterator();
    return new Iterator<T>() {
      private int position = 0;
      @Override
      public boolean hasNext() {
        return position < maxSize && internal.hasNext();
      }

      @Override
      public T next() {
        position++;
        return internal.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("read only iterator.");
      }
    };
  }

  public T peekFirst() {
    return window.peekFirst();
  }

  public T peekLast() {
    return window.peekLast();
  }

}
