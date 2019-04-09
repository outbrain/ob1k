package com.outbrain.ob1k.cache;

/**
 * wrapper for cache value that holds the write time of the value in the cache
 */
public class ValueWithWriteTime<V> {

  private V value;
  private long writeTime;

  /**
   * empty constructor for message pack
   */
  public ValueWithWriteTime() { }

  /**
   * creates an object with the given value and the current timestamp
   * @param value
   */
  public ValueWithWriteTime(final V value, final long writeTime) {
    this.value = value;
    this.writeTime = writeTime;
  }

  /**
   *
   * @return the actual value
   */
  public V getValue() {
    return value;
  }

  /**
   *
   * @return the write timestamp in milliseconds
   */
  public long getWriteTime() {
    return writeTime;
  }
}
