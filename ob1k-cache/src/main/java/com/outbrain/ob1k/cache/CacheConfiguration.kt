package com.outbrain.ob1k.cache


import com.outbrain.swinfra.metrics.api.MetricFactory
import java.util.concurrent.TimeUnit

data class CacheConfiguration<K, V>(val cacheName: String,
                                    var ttl: Int = 360_000,
                                    var ttlTimeUnit: TimeUnit = TimeUnit.MILLISECONDS,
                                    var loader: CacheLoader<K, V>? = null,
                                    var failOnMissingEntries: Boolean = false,
                                    var maxSize: Int = 1_000_000,
                                    var loadTimeout: Long = 500,
                                    var loadTimeUnit: TimeUnit = TimeUnit.MILLISECONDS,
                                    var metricFactory: MetricFactory? = null) {

  constructor(cacheName1: String) : this(cacheName = cacheName1)

  @JvmOverloads
  fun withTtl(ttl: Int, ttlTimeUnit: TimeUnit = TimeUnit.MILLISECONDS): CacheConfiguration<K, V> {
    this.ttl = ttl
    this.ttlTimeUnit = ttlTimeUnit
    return this
  }

  fun withLoader(loader: CacheLoader<K, V>): CacheConfiguration<K, V> {
    this.loader = loader
    return this
  }

  @JvmOverloads
  fun failOnMissingEntries(fail: Boolean = true): CacheConfiguration<K, V> {
    this.failOnMissingEntries = fail
    return this
  }

  fun withMaxSize(maxSize: Int): CacheConfiguration<K, V> {
    this.maxSize = maxSize
    return this
  }

  @JvmOverloads
  fun withLoadTimeout(loadTimeout: Long, loadTimeUnit: TimeUnit = TimeUnit.MILLISECONDS): CacheConfiguration<K, V> {
    this.loadTimeout = loadTimeout
    this.loadTimeUnit = loadTimeUnit
    return this
  }

  /**
   * for testing purposes
   */
  fun withMetricFactory(metricFactory: MetricFactory): CacheConfiguration<K, V> {
    this.metricFactory = metricFactory
    return this
  }

  fun buildLocalAsyncCache(): LocalAsyncCache<K, V> = LocalAsyncCache(this)

  fun buildLoadingCacheDelegate(cache: TypedCache<K, V>) = LoadingCacheDelegate(cache, this)

}
