package com.outbrain.cqllib;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.exceptions.Exceptions;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by guyk on 7/9/14
 */
public class CqlSession {
  private final Session session;
  private final RetryPolicy retryPolicy;
  private final MetricFactory metricFactory;

  private static final Logger logger = LoggerFactory.getLogger(CqlSession.class);

  public CqlSession(final String nodes, final int port, final String keyspace, final SocketOptions socketOptions,
                    final RetryPolicy retryPolicy, final QueryOptions queryOptions,
                    final LoadBalancingPolicy loadBalancingPolicy, final int maxConnectionsPerHost,
                    final MetricFactory metricFactory) {

    // this is temp. to reuse current hosts properties:
    final Iterable<String> nodesIter = Splitter.on(",").split(nodes);
    final String[] nodesArr = Iterables.toArray(Iterables.transform(nodesIter, new Function<String, String>() {
      @Override
      public String apply(final String input) {
        if (input == null) return null;

        final int idx = input.lastIndexOf(":");
        return input.substring(0, idx);
      }
    }), String.class);


    /*PoolingOptions poolingOptions = new PoolingOptions();
    poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, maxConnectionsPerHost);
    poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, maxConnectionsPerHost);*/


    final Cluster cluster = Cluster.builder().
            withPort(port).
            withSocketOptions(socketOptions).
            withQueryOptions(queryOptions).
            withLoadBalancingPolicy(loadBalancingPolicy).
            //  withPoolingOptions(poolingOptions).
                    addContactPoints(nodesArr).build();
    //cluster.init();
    this.session = cluster.connect(keyspace);
    this.retryPolicy = Preconditions.checkNotNull(retryPolicy);
    this.metricFactory = Preconditions.checkNotNull(metricFactory);
  }

  public CqlStatementFactory newFactory(final String... tags) {
    return new CqlStatementFactory(metricFactory, tags);
  }

  public CqlPreparedStatement prepare(final String query, final String... tags) {
    return new CqlPreparedStatement(session.prepare(query), metricFactory, tags);
  }

  public ComposableFuture<ResultSet> executeAsync(final CqlStatement cqlStatement) {
    return executeImpl(cqlStatement.getStatement(), cqlStatement.getTagMetrics());
  }

  public ComposableFuture<ResultSet> executeAsync(final CqlPreparedStatement cqlStatement, final Object... parameters) {
    return executeImpl(cqlStatement.getStatement().bind(parameters), cqlStatement.getTagMetrics());
  }

  private ComposableFuture<ResultSet> executeImpl(final Statement statement, final List<TagMetrics> tagMetrics) {
    statement.setRetryPolicy(new RetryPolicyWithMetrics(retryPolicy, tagMetrics));
    final Iterable<Timer.Context> timerContexts = measureOnStart(tagMetrics);
    final ResultSetFuture resultSetFuture = session.executeAsync(statement);
    return fromListenableFuture(resultSetFuture, tagMetrics).continueWith(new FutureResultHandler<ResultSet, ResultSet>() {
      @Override
      public ComposableFuture<ResultSet> handle(final ComposableFuture<ResultSet> result) {
        measureOnDone(timerContexts);
        return result;
      }
    });
  }


  private List<Timer.Context> measureOnStart(final Iterable<TagMetrics> tagMetrics) {
    return Lists.newArrayList(Iterables.transform(tagMetrics, new Function<TagMetrics, Timer.Context>() {
      @Override
      public Timer.Context apply(final TagMetrics input) {
        return input != null ? input.timer.time() : null;
      }
    }));
  }

  private void measureOnDone(final Iterable<Timer.Context> timerContexts) {
    for (final Timer.Context timerContext : timerContexts) {
      timerContext.stop();
    }
  }

  private static <T> ComposableFuture<T> fromListenableFuture(final ListenableFuture<T> source, final List<TagMetrics> tagMetrics) {
    final ComposablePromise<T> res = ComposableFutures.newPromise(false);
    source.addListener(new Runnable() {
      @Override
      public void run() {
        try {
          final T result = source.get();
          res.set(result);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          res.setException(e);
        } catch (final ExecutionException e) {
          final Throwable finalCause = Exceptions.getFinalCause(e);
          if (finalCause instanceof NoHostAvailableException) {
            final String tags = Joiner.on(',').join(tagMetrics);
            final Map<InetSocketAddress, Throwable> errorsPerHost = ((NoHostAvailableException) finalCause).getErrors();
            for (final Map.Entry<InetSocketAddress, Throwable> entry : errorsPerHost.entrySet()) {
              logger.error("host " + entry.getKey() + " failed to perform statement " + tags + ": " +
                  entry.getValue().getMessage(), entry.getValue().getMessage());
            }
          }
          res.setException(e.getCause() != null ? e.getCause() : e);
        }
      }
    }, ComposableFutures.getExecutor());

    return res;
  }
}

