package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.mysql.MySQLConnection;
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory;
import com.github.mauricio.async.db.pool.AsyncObjectPool;
import com.github.mauricio.async.db.pool.ConnectionPool;
import com.github.mauricio.async.db.pool.PoolConfiguration;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.handlers.*;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;
import scala.concurrent.Future;

import java.util.List;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * User: aronen
 * Date: 9/22/13
 * Time: 5:59 PM
 */
class MySqlConnectionPool implements DbConnectionPool {
  private static final Logger logger = LoggerFactory.getLogger(MySqlConnectionPool.class);

  private final ConnectionPool<MySQLConnection> _pool;

  MySqlConnectionPool(final MySQLConnectionFactory connFactory, final PoolConfiguration poolConfiguration, final MetricFactory metricFactory) {
    _pool = new ConnectionPool<>(connFactory, poolConfiguration, ScalaFutureHelper.ctx);
    initializeMetrics(metricFactory, _pool);
  }


  private static void initializeMetrics(final MetricFactory metricFactory, final ConnectionPool<MySQLConnection> pool) {
    if (metricFactory != null) {
      metricFactory.registerGauge("MysqlAsyncConnectionPool", "available", () -> pool.availables().size());
      metricFactory.registerGauge("MysqlAsyncConnectionPool", "waiting", () -> pool.queued().size());
      metricFactory.registerGauge("MysqlAsyncConnectionPool", "inUse", () -> pool.inUse().size());
    }
  }

  @Override
  public ComposableFuture<QueryResult> sendQuery(final String query) {
    return ScalaFutureHelper.from(() -> _pool.sendQuery(query));
  }

  @Override
  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return ScalaFutureHelper.from(() -> _pool.sendPreparedStatement(query, scalaValues));
  }

  private ComposableFuture<MySqlAsyncConnection> take() {
    final ComposableFuture<MySQLConnection> connFuture = ScalaFutureHelper.from(_pool::take);

    return connFuture.continueOnSuccess((SuccessHandler<MySQLConnection, MySqlAsyncConnection>) MySqlAsyncConnection::new);
  }

  private ComposableFuture<Boolean> giveBack(final MySqlAsyncConnection conn) {
    return ScalaFutureHelper.from(() -> _pool.giveBack(conn.getInnerConnection())).continueWith((ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>) Try::isSuccess);
  }

  @Override
  public <T> ComposableFuture<T> withConnection(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.continueOnSuccess((FutureSuccessHandler<MySqlAsyncConnection, T>) conn -> handler.handle(conn).continueWith((FutureResultHandler<T, T>) result -> giveBack(conn).continueWith((FutureResultHandler<Boolean, T>) giveBackResult -> ComposableFutures.fromTry(result))));
  }

  @Override
  public <T> ComposableFuture<T> withTransaction(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.continueOnSuccess((FutureSuccessHandler<MySqlAsyncConnection, T>) conn -> conn.startTx().continueOnSuccess((FutureSuccessHandler<MySqlAsyncConnection, T>) result -> handler.handle(conn)).continueOnSuccess((FutureSuccessHandler<T, T>) result -> conn.commit().continueWith((FutureResultHandler<QueryResult, T>) commitResult -> giveBack(conn).continueWith((FutureResultHandler<Boolean, T>) giveBackResult -> {
      if (!giveBackResult.isSuccess()) {
        logger.warn("can't return connection back to pool", giveBackResult.getError());
      }

      if (commitResult.isSuccess()) {
        return fromValue(result);
      } else {
        return fromError(commitResult.getError());
      }
    }))).continueOnError((FutureErrorHandler<T>) error -> conn.rollBack().continueWith((FutureResultHandler<QueryResult, T>) rollBackResult -> giveBack(conn).continueWith((FutureResultHandler<Boolean, T>) result -> {
      if (!result.isSuccess()) {
        logger.warn("can't return connection back to pool", error);
      }
      return fromError(error);
    }))));
  }

  @Override
  public ComposableFuture<Boolean> close() {
    final ComposableFuture<AsyncObjectPool<MySQLConnection>> future = ScalaFutureHelper.from(_pool::close);

    return future.continueWith((ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>) result -> {
      try {
        return result.isSuccess();
      } catch (final Exception e) {
        return false;
      }
    });
  }

}
