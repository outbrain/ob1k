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
public class MySqlConnectionPool {
  private static final Logger logger = LoggerFactory.getLogger(MySqlConnectionPool.class);
  public static final int DEFAULT_MAX_IDLE_TIME = 15 * 60 * 1000;
  public static final int DEFAULT_VALIDATION_INTERVAL = 30 * 1000;

  private final ConnectionPool<MySQLConnection> _pool;

  public MySqlConnectionPool(final MySQLConnectionFactory connFactory, final int maxConnections, final MetricFactory metricFactory) {
    final PoolConfiguration configuration = new PoolConfiguration(maxConnections, DEFAULT_MAX_IDLE_TIME, 10, DEFAULT_VALIDATION_INTERVAL);
    _pool = new ConnectionPool<>(connFactory, configuration, ScalaFutureHelper.ctx);
    initializeMetrics(metricFactory, _pool);
  }

  public MySqlConnectionPool(final String host, final int port, final String database, final String userName,
                             final String password, final MetricFactory metricFactory) {
    this(host, port, database, userName, password, 10, metricFactory);
  }

  public MySqlConnectionPool(final String host, final int port, final String database, final String userName,
                             final String password, final int maxConnections, final MetricFactory metricFactory) {
    this(host, port, database, userName, password, maxConnections, 2 /* sec */, DEFAULT_MAX_IDLE_TIME, maxConnections * 2,
        DEFAULT_VALIDATION_INTERVAL, metricFactory);
  }

  public MySqlConnectionPool(final String host, final int port, final String database, final String userName,
                             final String password, final int maxConnections, final long connectTimeoutSeconds, final long maxIdleTimeMs,
                             final int maxQueueSize, final long validationIntervalMs, final MetricFactory metricFactory) {

    final MySQLConnectionFactory connFactory =
        new MySQLConnectionFactory(MySqlAsyncConnection.createConfiguration(host, port, database, userName, password, connectTimeoutSeconds));
    final PoolConfiguration configuration = new PoolConfiguration(maxConnections, maxIdleTimeMs, maxQueueSize, validationIntervalMs);
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
    return ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<QueryResult>() {
      @Override
      public Future<QueryResult> provide() {
        return _pool.sendQuery(query);
      }
    });
  }

  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<QueryResult>() {
      @Override
      public Future<QueryResult> provide() {
        return _pool.sendPreparedStatement(query, scalaValues);
      }
    });
  }

  private ComposableFuture<MySqlAsyncConnection> take() {
    final ComposableFuture<MySQLConnection> connFuture = ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<MySQLConnection>() {
      @Override
      public Future<MySQLConnection> provide() {
        return _pool.take();
      }
    });

    return connFuture.continueOnSuccess(new SuccessHandler<MySQLConnection, MySqlAsyncConnection>() {
      @Override
      public MySqlAsyncConnection handle(final MySQLConnection result) {
        return new MySqlAsyncConnection(result);
      }
    });
  }

  private ComposableFuture<Boolean> giveBack(final MySqlAsyncConnection conn) {
    return ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<AsyncObjectPool<MySQLConnection>>() {
      @Override
      public Future<AsyncObjectPool<MySQLConnection>> provide() {
        return _pool.giveBack(conn.getInnerConnection());
      }
    }).continueWith(new ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>() {
      @Override
      public Boolean handle(final Try<AsyncObjectPool<MySQLConnection>> result) {
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
          public ComposableFuture<T> handle(final Try<T> result) {
            return giveBack(conn).continueWith(new FutureResultHandler<Boolean, T>() {
              @Override
              public ComposableFuture<T> handle(final Try<Boolean> giveBackResult) {
                return ComposableFutures.fromTry(result);
              }
            });
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
              public ComposableFuture<T> handle(final Try<QueryResult> commitResult) {
                return giveBack(conn).continueWith(new FutureResultHandler<Boolean, T>() {
                  @Override
                  public ComposableFuture<T> handle(final Try<Boolean> giveBackResult) {
                    if (!giveBackResult.isSuccess()) {
                      logger.warn("can't return connection back to pool", giveBackResult.getError());
                    }

                    if (commitResult.isSuccess()) {
                      return fromValue(result);
                    } else {
                      return fromError(commitResult.getError());
                    }
                  }
                });
              }
            });
          }
        }).continueOnError(new FutureErrorHandler<T>() {
          @Override
          public ComposableFuture<T> handle(final Throwable error) {
            return conn.rollBack().continueWith(new FutureResultHandler<QueryResult, T>() {
              @Override
              public ComposableFuture<T> handle(final Try<QueryResult> rollBackResult) {
                return giveBack(conn).continueWith(new FutureResultHandler<Boolean, T>() {
                  @Override
                  public ComposableFuture<T> handle(final Try<Boolean> result) {
                    if (!result.isSuccess()) {
                      logger.warn("can't return connection back to pool", error);
                    }
                    return fromError(error);
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  public ComposableFuture<Boolean> close() {
    final ComposableFuture<AsyncObjectPool<MySQLConnection>> future = ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<AsyncObjectPool<MySQLConnection>>() {
      @Override
      public Future<AsyncObjectPool<MySQLConnection>> provide() {
        return _pool.close();
      }
    });

    return future.continueWith(new ResultHandler<AsyncObjectPool<MySQLConnection>, Boolean>() {
      @Override
      public Boolean handle(final Try<AsyncObjectPool<MySQLConnection>> result) {
        try {
          return result.isSuccess();
        } catch (final Exception e) {
          return false;
        }
      }
    });
  }

}
