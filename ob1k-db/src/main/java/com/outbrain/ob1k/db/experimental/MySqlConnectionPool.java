package com.outbrain.ob1k.db.experimental;

import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory;
import com.github.jasync.sql.db.pool.AsyncObjectPool;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.pool.PoolConfiguration;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    _pool = new ConnectionPool<>(connFactory, poolConfiguration, ComposableFutures.getExecutor());
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
    return JavaFutureHelper.from(() -> _pool.sendQuery(query));
  }

  @Override
  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    return JavaFutureHelper.from(() -> _pool.sendPreparedStatement(query, values));
  }

  private ComposableFuture<MySqlAsyncConnection> take() {
    final ComposableFuture<MySQLConnection> connFuture = JavaFutureHelper.from(_pool::take);

    return connFuture.map(MySqlAsyncConnection::new);
  }

  private ComposableFuture<Boolean> giveBack(final MySqlAsyncConnection conn) {
    return JavaFutureHelper.from(() -> _pool.giveBack(conn.getInnerConnection())).always(Try::isSuccess);
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
    final ComposableFuture<AsyncObjectPool<MySQLConnection>> future = JavaFutureHelper.from(_pool::close);
    return future.always(Try::isSuccess);
  }
}
