package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.mysql.MySQLConnection;
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory;
import com.github.mauricio.async.db.pool.AsyncObjectPool;
import com.github.mauricio.async.db.pool.ConnectionPool;
import com.github.mauricio.async.db.pool.PoolConfiguration;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

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
  private final MetricFactory metricFactory;
  private final Counter sendQueryCounter;
  private final Timer sendQueryTimer;
  private final Counter preparedStatementCounter;
  private final Timer preparedStatementTimer;

  MySqlConnectionPool(final MySQLConnectionFactory connFactory, final PoolConfiguration poolConfiguration, final MetricFactory metricFactory) {
    _pool = new ConnectionPool<>(connFactory, poolConfiguration, ScalaFutureHelper.ctx);
    this.metricFactory = metricFactory;
    if (metricFactory != null) {
      metricFactory.registerGauge("old_MysqlAsyncConnectionPool", "available", () -> _pool.availables().size());
      metricFactory.registerGauge("old_MysqlAsyncConnectionPool", "waiting", () -> _pool.queued().size());
      metricFactory.registerGauge("old_MysqlAsyncConnectionPool", "inUse", () -> _pool.inUse().size());
      sendQueryCounter = metricFactory.createCounter("old_MysqlAsyncConnectionPool", "sendQueryCounter");
      sendQueryTimer = metricFactory.createTimer("old_MysqlAsyncConnectionPool", "sendQueryTimer");
      preparedStatementCounter = metricFactory.createCounter("old_MysqlAsyncConnectionPool", "preparedStatementCounter");
      preparedStatementTimer = metricFactory.createTimer("old_MysqlAsyncConnectionPool", "preparedStatementTimer");
    } else {
      sendQueryCounter = null;
      sendQueryTimer = null;
      preparedStatementCounter = null;
      preparedStatementTimer = null;
    }
  }

  private <T> ComposableFuture<T> withMetricsSendQuery(ComposableFuture<T> future) {
    if (metricFactory != null) {
      sendQueryCounter.inc();
      final Timer.Context timer = sendQueryTimer.time();
      future.andThen(tried -> {
        timer.stop();
      });
    }
    return future;
  }
  private <T> ComposableFuture<T> withMetricsPreparedStatement(ComposableFuture<T> future) {
    if (metricFactory != null) {
      preparedStatementCounter.inc();
      final Timer.Context timer = preparedStatementTimer.time();
      future.andThen(tried -> {
        timer.stop();
      });
    }
    return future;
  }
  @Override
  public ComposableFuture<QueryResult> sendQuery(final String query) {
    return withMetricsSendQuery(ScalaFutureHelper.from(() -> _pool.sendQuery(query)));
  }

  @Override
  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return withMetricsPreparedStatement(ScalaFutureHelper.from(() -> _pool.sendPreparedStatement(query, scalaValues)));
  }

  private ComposableFuture<MySqlAsyncConnection> take() {
    final ComposableFuture<MySQLConnection> connFuture = ScalaFutureHelper.from(_pool::take);

    return connFuture.map(MySqlAsyncConnection::new);
  }

  private ComposableFuture<Boolean> giveBack(final MySqlAsyncConnection conn) {
    return ScalaFutureHelper.from(() -> _pool.giveBack(conn.getInnerConnection())).always(Try::isSuccess);
  }

  @Override
  public <T> ComposableFuture<T> withConnection(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.flatMap(conn ->
      handler.handle(conn).
        alwaysWith(result -> giveBack(conn).
          alwaysWith(giveBackResult -> ComposableFutures.fromTry(result))));
  }

  @Override
  public <T> ComposableFuture<T> withTransaction(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.flatMap(conn -> conn.startTx().
      flatMap(result -> handler.handle(conn)).
      flatMap(result -> conn.commit().
        alwaysWith(commitResult -> giveBack(conn).
          alwaysWith(giveBackResult -> {
            if (!giveBackResult.isSuccess()) {
              logger.warn("can't return connection back to pool", giveBackResult.getError());
            }

            if (commitResult.isSuccess()) {
              return fromValue(result);
            } else {
              return fromError(commitResult.getError());
            }
          })
        )
      ).recoverWith(error -> conn.rollBack().alwaysWith(rollBackResult -> giveBack(conn).
        alwaysWith(result -> {
          if (!result.isSuccess()) {
            logger.warn("can't return connection back to pool", error);
          }
          return fromError(error);
        })
      )
    ));
  }

  @Override
  public ComposableFuture<Boolean> close() {
    final ComposableFuture<AsyncObjectPool<MySQLConnection>> future = ScalaFutureHelper.from(_pool::close);
    return future.always(Try::isSuccess);
  }
}
