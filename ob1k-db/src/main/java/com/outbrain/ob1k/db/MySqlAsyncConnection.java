package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.Configuration;
import com.github.mauricio.async.db.Connection;
import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.mysql.MySQLConnection;
import com.github.mauricio.async.db.mysql.util.CharsetMapper;
import com.github.mauricio.async.db.util.NettyUtils;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import scala.Option;
import scala.Some;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * User: aronen
 * Date: 9/17/13
 * Time: 4:55 PM
 */
public class MySqlAsyncConnection {
  private final MySQLConnection conn;

  public MySqlAsyncConnection(final MySQLConnection conn) {
    this.conn = conn;
  }

  public MySqlAsyncConnection(final String host, final int port, final String database, final String userName, final String password) {
    final Configuration conf = createConfiguration(host, port, database, userName, password);
    conn = new MySQLConnection(conf, CharsetMapper.Instance(), NettyUtils.DefaultEventLoopGroup(), ScalaFutureHelper.ctx);
  }

  public static Configuration createConfiguration(final String host, final int port, final String database, final String userName, final String password) {
    final Option<String> empty = Option.apply(null);
    final Option<String> dbOption = database != null ? new Some<>(database) : empty;
    final Option<String> passOption = password != null ? new Some<>(password) : empty;
    return new Configuration(userName, host, port, passOption, dbOption, CharsetUtil.UTF_8, 16777216,
        PooledByteBufAllocator.DEFAULT, Duration.apply(2, TimeUnit.SECONDS), Duration.apply(4, TimeUnit.SECONDS));
  }

  MySQLConnection getInnerConnection() {
    return conn;
  }

  public ComposableFuture<QueryResult> sendQuery(final String query) {
    return ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<QueryResult>() {
      @Override
      public Future<QueryResult> provide() {
        return conn.sendQuery(query);
      }
    });
  }

  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<QueryResult>() {
      @Override
      public Future<QueryResult> provide() {
        return conn.sendPreparedStatement(query, scalaValues);
      }
    });
  }

  public ComposableFuture<MySqlAsyncConnection> connect() {
    if (conn.isConnected()) {
      return ComposableFutures.fromValue(this);
    }

    final ComposableFuture<Connection> composableFuture = ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<Connection>() {
      @Override
      public Future<Connection> provide() {
        return conn.connect();
      }
    });

    return composableFuture.continueOnSuccess(new SuccessHandler<Connection, MySqlAsyncConnection>() {
      @Override
      public MySqlAsyncConnection handle(final Connection result) {
        final MySQLConnection connection = (MySQLConnection) result;
        if (connection == conn) {
          return MySqlAsyncConnection.this;
        } else {
          return new MySqlAsyncConnection(connection);
        }
      }
    });
  }

  public ComposableFuture<MySqlAsyncConnection> startTx() {
    return this.sendQuery("START TRANSACTION;").continueOnSuccess(new FutureSuccessHandler<QueryResult, MySqlAsyncConnection>() {
      @Override
      public ComposableFuture<MySqlAsyncConnection> handle(final QueryResult result) {
        return ComposableFutures.fromValue(MySqlAsyncConnection.this);
      }
    });
  }

  public ComposableFuture<QueryResult> commit() {
    return this.sendQuery("COMMIT;");
  }

  public ComposableFuture<QueryResult> rollBack() {
    return this.sendQuery("ROLLBACK;");
  }

  public ComposableFuture<MySqlAsyncConnection> disconnect() {
    final ComposableFuture<Connection> composableFuture = ScalaFutureHelper.from(new ScalaFutureHelper.FutureProvider<Connection>() {
      @Override
      public Future<Connection> provide() {
        return conn.disconnect();
      }
    });

    return composableFuture.continueOnSuccess(new SuccessHandler<Connection, MySqlAsyncConnection>() {
      @Override
      public MySqlAsyncConnection handle(final Connection result) {
        final MySQLConnection connection = (MySQLConnection) result;
        if (connection == conn) {
          return MySqlAsyncConnection.this;
        } else {
          return new MySqlAsyncConnection(connection);
        }
      }
    });
  }

}
