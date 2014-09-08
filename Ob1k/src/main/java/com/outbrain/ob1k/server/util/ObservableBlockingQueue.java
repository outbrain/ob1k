package com.outbrain.ob1k.server.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

/**
 * Time: 5/2/14 8:43 PM
 *
 * @author Eran Harel
 */
public class ObservableBlockingQueue<E> implements BlockingQueue<E> {
  private static final double NOTIFY_THRESHOLD_FACTOR = 0.4;
  private final BlockingQueue<E> delegate;
  private final int notifyThreshold;
  private final QueueObserver observer;

  public ObservableBlockingQueue(final BlockingQueue delegate, final QueueObserver observer) {
    this.observer = Preconditions.checkNotNull(observer, "observer must not be null");
    this.delegate = Preconditions.checkNotNull(delegate, "delegate must not be null");
    this.notifyThreshold = (int) (delegate.remainingCapacity() * NOTIFY_THRESHOLD_FACTOR);
  }

  @Override
  public boolean add(final E e) {
    return delegate.add(e);
  }

  @Override
  public boolean offer(final E e) {
    return delegate.offer(e);
  }

  @Override
  public E remove() {
    final E e = delegate.remove();
    notifyIfNeeded(e);
    return e;
  }

  @Override
  public E poll() {
    final E e = delegate.poll();
    notifyIfNeeded(e);
    return e;
  }

  @Override
  public E element() {
    return null;
  }

  @Override
  public E peek() {
    return delegate.peek();
  }

  @Override
  public void put(final E e) throws InterruptedException {
    delegate.put(e);
  }

  @Override
  public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
    return delegate.offer(e);
  }

  @Override
  public E take() throws InterruptedException {
    final E e = delegate.take();
    notifyIfNeeded(e);
    return e;
  }

  @Override
  public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
    final E e = delegate.poll(timeout, unit);
    notifyIfNeeded(e);
    return e;
  }

  @Override
  public int remainingCapacity() {
    return delegate.remainingCapacity();
  }

  @Override
  public boolean remove(final Object o) {
    final boolean removed = delegate.remove(o);
    notifyIfNeeded(removed);
    return removed;
  }

  @Override
  public boolean containsAll(final Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(final Collection<? extends E> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean removeAll(final Collection<?> c) {
    final boolean removed = delegate.removeAll(c);
    notifyIfNeeded(removed);
    return removed;
  }

  @Override
  public boolean retainAll(final Collection<?> c) {
    final boolean removed = delegate.retainAll(c);
    notifyIfNeeded(removed);
    return removed;
  }

  @Override
  public void clear() {
    delegate.clear();
    notifyIfNeeded(true);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(final Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    return delegate.iterator();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(final T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public int drainTo(final Collection<? super E> c) {
    final int drained = delegate.drainTo(c);
    notifyIfNeeded(0 < drained);
    return drained;
  }

  @Override
  public int drainTo(final Collection<? super E> c, final int maxElements) {
    final int drained = delegate.drainTo(c, maxElements);
    notifyIfNeeded(0 < drained);
    return drained;
  }

  private void notifyIfNeeded(final E e) {
    notifyIfNeeded(e != null);
  }

  private void notifyIfNeeded(final boolean elementRemoved) {
    if(elementRemoved && delegate.size() <= notifyThreshold) {
      observer.onQueueSizeBelowThreshold();
    }
  }
}
