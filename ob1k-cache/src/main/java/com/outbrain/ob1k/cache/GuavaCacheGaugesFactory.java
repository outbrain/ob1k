package com.outbrain.ob1k.cache;

import com.google.common.cache.Cache;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;


/**
 * Created by aronen on 2/20/14.
 * creates a set of gauges that monitors Guava cache.
 *
 */
public class GuavaCacheGaugesFactory {
  public static void createGauges(final MetricFactory metricFactory, final Cache cache, final String cacheName) {
    if (metricFactory != null) {
      metricFactory.registerGauge(cacheName, "averageLoadPenalty", new Gauge<Double>() {
        @Override
        public Double getValue() {
          return cache.stats().averageLoadPenalty();
        }
      });

      metricFactory.registerGauge(cacheName, "hitRate", new Gauge<Double>() {
        @Override
        public Double getValue() {
          return cache.stats().hitRate();
        }
      });

      metricFactory.registerGauge(cacheName, "loadExceptionRate", new Gauge<Double>() {
        @Override
        public Double getValue() {
          return cache.stats().loadExceptionRate();
        }
      });

      metricFactory.registerGauge(cacheName, "missRate", new Gauge<Double>() {
        @Override
        public Double getValue() {
          return cache.stats().missRate();
        }
      });

      metricFactory.registerGauge(cacheName, "evictionCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().evictionCount();
        }
      });

      metricFactory.registerGauge(cacheName, "hitCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().hitCount();
        }
      });

      metricFactory.registerGauge(cacheName, "loadCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().loadCount();
        }
      });

      metricFactory.registerGauge(cacheName, "loadExceptionCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().loadExceptionCount();
        }
      });

      metricFactory.registerGauge(cacheName, "loadSuccessCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().loadSuccessCount();
        }
      });

      metricFactory.registerGauge(cacheName, "missCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().missCount();
        }
      });

      metricFactory.registerGauge(cacheName, "requestCount", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().requestCount();
        }
      });

      metricFactory.registerGauge(cacheName, "totalLoadTime", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.stats().totalLoadTime();
        }
      });

      metricFactory.registerGauge(cacheName, "size", new Gauge<Long>() {
        @Override
        public Long getValue() {
          return cache.size();
        }
      });

    }

  }
}
