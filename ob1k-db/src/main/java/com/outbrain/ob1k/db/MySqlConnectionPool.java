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
import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 9/22/13
 * Time: 5:59 PM
 */
public class MySqlConnectionPool {
  private static final Logger logger = LoggerFactory.getLogger(MySqlConnectionPool.class);

  private final ConnectionPool<MySQLConnection> _pool;

  public MySqlConnectionPool(MySQLConnectionFactory connFactory, final int maxConnections, MetricFactory metricFactory) {
    final PoolConfiguration configuration = new PoolConfiguration(maxConnections, 4, 10, 5000);
    _pool = new ConnectionPool<>(connFactory, configuration, ScalaFutureHelper.ctx);
    initializeMetrics(metricFactory, _pool);
  }

  public MySqlConnectionPool(final String host, final int port, final String database, final String userName,
                             final String password, final MetricFactory metricFactory) {
    this(host, port, database, userName, password, 10, metricFactory);
  }

  public MySqlConnectionPool(final String host, final int port, final String database, final String userName,
                             final String password, final int maxConnections, final MetricFactory metricFactory) {
    final MySQLConnectionFactory connFactory =
        new MySQLConnectionFactory(MySqlAsyncConnection.createConfiguration(host, port, database, userName, password));
    final PoolConfiguration configuration = new PoolConfiguration(maxConnections, 4, 10, 5000);
    _pool = new ConnectionPool<>(connFactory, configuration, ScalaFutureHelper.ctx);

    initializeMetrics(metricFactory, _pool);
  }

  private static void initializeMetrics(final MetricFactory metricFactory, final ConnectionPool<MySQLConnection> pool) {
    if (metricFactory != null) {
      metricFactory.registerGauge("MysqlAsyncConnectionPool", "available", new Gauge<Integer>() {
        @Override
        public Integer getValue() {
          return pool.availables().size();
        }
      });

      metricFactory.registerGauge("MysqlAsyncConnectionPool", "waiting", new Gauge<Integer>() {
        @Override
        public Integer getValue() {
          return pool.queued().size();
        }
      });

      metricFactory.registerGauge("MysqlAsyncConnectionPool", "inUse", new Gauge<Integer>() {
        @Override
        public Integer getValue() {
          return pool.inUse().size();
        }
      });
    }
  }

  public ComposableFuture<QueryResult> sendQuery(final String query) {
    final Future<QueryResult> res = _pool.sendQuery(query);
    return ScalaFutureHelper.from(res);
  }

  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return ScalaFutureHelper.from(_pool.sendPreparedStatement(query, scalaValues));
  }

  private ComposableFuture<MySqlAsyncConnection> take() {
    final ComposableFuture<MySQLConnection> connFuture = ScalaFutureHelper.from(_pool.take());
    return connFuture.continueOnSuccess(new SuccessHandler<MySQLConnection, MySqlAsyncConnection>() {
      @Override
      public MySqlAsyncConnection handle(final MySQLConnection result) throws ExecutionException {
        return new MySqlAsyncConnection(result);
      }
    });
  }

  private ComposableFuture<Boolean> giveBack(final MySqlAsyncConnection conn) {
    final Future<AsyncObjectPool<MySQLConnection>> futureRes = _pool.giveBack(conn.getInnerConnection());
    return ScalaFutureHelper.from(futureRes).continueWith(new ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>() {
      @Override
      public Boolean handle(final ComposableFuture<AsyncObjectPool<MySQLConnection>> result) throws ExecutionException {
        return result.isSuccess();
      }
    });
  }

  public <T> ComposableFuture<T> withConnection(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.continueOnSuccess(new FutureSuccessHandler<MySqlAsyncConnection, T>() {
      @Override
      public ComposableFuture<T> handle(final MySqlAsyncConnection conn) {
        return handler.handle(conn).continueWith(new FutureResultHandler<T, T>() {
          @Override
          public ComposableFuture<T> handle(final ComposableFuture<T> result) {
            giveBack(conn);
            return result;
          }
        });
      }
    });
  }

  public <T> ComposableFuture<T> withTransaction(final TransactionHandler<T> handler) {
    final ComposableFuture<MySqlAsyncConnection> futureConn = take();
    return futureConn.continueOnSuccess(new FutureSuccessHandler<MySqlAsyncConnection, T>() {
      @Override
      public ComposableFuture<T> handle(final MySqlAsyncConnection conn) {
        return conn.startTx().continueOnSuccess(new FutureSuccessHandler<MySqlAsyncConnection, T>() {
          @Override
          public ComposableFuture<T> handle(final MySqlAsyncConnection result) {
            return handler.handle(conn);
          }
        }).continueOnSuccess(new FutureSuccessHandler<T, T>() {
          @Override
          public ComposableFuture<T> handle(final T result) {
            return conn.commit().continueWith(new FutureResultHandler<QueryResult, T>() {
              @Override
              public ComposableFuture<T> handle(final ComposableFuture<QueryResult> commitResult) {
                giveBack(conn).onError(new OnErrorHandler() {
                  @Override
                  public void handle(final Throwable error) {
                    logger.warn("can't return connection back to pool", error);
                  }
                });

                if (commitResult.isSuccess()) {
                  return ComposableFutures.fromValue(result);
                } else {
                  return ComposableFutures.fromError(commitResult.getError());
                }
              }
            });
          }
        }).continueOnError(new FutureErrorHandler<T>() {
          @Override
          public ComposableFuture<T> handle(final Throwable error) {
            return conn.rollBack().continueWith(new FutureResultHandler<QueryResult, T>() {
              @Override
              public ComposableFuture<T> handle(final ComposableFuture<QueryResult> rollBackResult) {
                giveBack(conn).onError(new OnErrorHandler() {
                  @Override
                  public void handle(final Throwable error) {
                    logger.warn("can't return connection back to pool", error);
                  }
                });

                return ComposableFutures.fromError(error);
              }
            });
          }
        });
      }
    });
  }


  public ComposableFuture<Boolean> close() {
    final ComposableFuture<AsyncObjectPool<MySQLConnection>> future = ScalaFutureHelper.from(_pool.close());
    return future.continueWith(new ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>() {
      @Override
      public Boolean handle(final ComposableFuture<AsyncObjectPool<MySQLConnection>> result) throws ExecutionException {
        try {
          return result.isSuccess();
        } catch (final Exception e) {
          return false;
        }
      }
    });
  }

}
