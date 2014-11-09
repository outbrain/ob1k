package com.outbrain.ob1k.server.jetty.metrics;

import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Creates gauges for the Jetty Server statistics
 *
 * @author Eran Harel
 */
public class StatisticsGuagesFactory {

  public static final String SERVER_STATS = "ServerStatistics";

  public static void createGauges(final StatisticsHandler stats, final MetricFactory metricFactory) {
    createResponsesGauges(stats, metricFactory);
    createRequestsGauges(stats, metricFactory);
  }

  private static void createRequestsGauges(final StatisticsHandler stats, final MetricFactory metricFactory) {
    metricFactory.registerGauge(SERVER_STATS, "requests", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getRequests();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "requestsActive", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getRequestsActive();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "requestTimeMax", new Gauge<Long>() {
      @Override
      public Long getValue() {
        return stats.getRequestTimeMax();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "requestTimeMean", new Gauge<Double>() {
      @Override
      public Double getValue() {
        return stats.getRequestTimeMean();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "requestTimeTotal", new Gauge<Long>() {
      @Override
      public Long getValue() {
        return stats.getRequestTimeTotal();
      }
    });
  }

  private static void createResponsesGauges(final StatisticsHandler stats, final MetricFactory metricFactory) {
    metricFactory.registerGauge(SERVER_STATS, "responsesBytesTotal", new Gauge<Long>() {
      @Override
      public Long getValue() {
        return stats.getResponsesBytesTotal();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "responses1xx", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getResponses1xx();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "responses2xx", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getResponses2xx();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "responses3xx", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getResponses3xx();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "responses4xx", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getResponses4xx();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "responses5xx", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getResponses5xx();
      }
    });
  }

  public static void createGauges(final ConnectorStatistics stats, final String connectorType, final MetricFactory metricFactory) {
    final String CONNECTOR_STATS = "ConnectorStats." + connectorType;

    metricFactory.registerGauge(CONNECTOR_STATS, "connectionDurationMax", new Gauge<Long>() {
      @Override
      public Long getValue() {
        return stats.getConnectionDurationMax();
      }
    });

    metricFactory.registerGauge(CONNECTOR_STATS, "connectionDurationMean", new Gauge<Double>() {
      @Override
      public Double getValue() {
        return stats.getConnectionDurationMean();
      }
    });

    metricFactory.registerGauge(CONNECTOR_STATS, "connections", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getConnections();
      }
    });

    metricFactory.registerGauge(CONNECTOR_STATS, "connectionsOpenMax", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getConnectionsOpenMax();
      }
    });

    metricFactory.registerGauge(CONNECTOR_STATS, "connectionsOpen", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return stats.getConnectionsOpen();
      }
    });
  }

  public static void createQTPGauges(final QueuedThreadPool qtp, final MetricFactory metricFactory) {
    metricFactory.registerGauge(SERVER_STATS, "idleThreads", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return qtp.getIdleThreads();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "totalThreads", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return qtp.getThreads();
      }
    });

    metricFactory.registerGauge(SERVER_STATS, "queueSize", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return qtp.getQueueSize();
      }
    });
  }
}
