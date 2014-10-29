package com.outbrain.ob1k.cache.memcache;

/**
 * Created by aronen on 10/12/14.
 */
public interface CacheKeyTranslator<K> {
  String translateKey(final K key);
}
