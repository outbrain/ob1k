package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.Configuration;
import com.github.mauricio.async.db.Connection;
import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.mysql.MySQLConnection;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import scala.Option;
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
  public static final int MAXIMUM_MESSAGE_SIZE = 16*1024*1024;
  private final MySQLConnection conn;

  public MySqlAsyncConnection(final MySQLConnection conn) {
    this.conn = conn;
  }

  public static Configuration createConfiguration(final String host, final int port, final Option<String> database, final String userName, final Option<String> password,
                                                  final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) {
    final Option<Duration> queryTimeout = queryTimeoutMilliSeconds == -1 ?
      Option.<Duration>apply(null) :
      Option.<Duration>apply(Duration.apply(queryTimeoutMilliSeconds, TimeUnit.MILLISECONDS));

    return new Configuration(userName, host, port, password, database, CharsetUtil.UTF_8, MAXIMUM_MESSAGE_SIZE,
      PooledByteBufAllocator.DEFAULT, Duration.apply(connectTimeoutMilliSeconds, TimeUnit.MILLISECONDS), Duration.apply(4, TimeUnit.SECONDS),
      queryTimeout);
  }

  MySQLConnection getInnerConnection() {
    return conn;
  }

  public ComposableFuture<QueryResult> sendQuery(final String query) {
    return ScalaFutureHelper.from(() -> conn.sendQuery(query));
  }

  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    final Buffer<Object> scalaValues = JavaConversions.asScalaBuffer(values);
    return ScalaFutureHelper.from(() -> conn.sendPreparedStatement(query, scalaValues));
  }

  public ComposableFuture<MySqlAsyncConnection> connect() {
    if (conn.isConnected()) {
      return ComposableFutures.fromValue(this);
    }

    final ComposableFuture<Connection> composableFuture = ScalaFutureHelper.from(conn::connect);

    return composableFuture.continueOnSuccess((SuccessHandler<Connection, MySqlAsyncConnection>) result -> {
      final MySQLConnection connection = (MySQLConnection) result;
      if (connection == conn) {
        return MySqlAsyncConnection.this;
      } else {
        return new MySqlAsyncConnection(connection);
      }
    });
  }

  public ComposableFuture<MySqlAsyncConnection> startTx() {
    return this.sendQuery("START TRANSACTION;").continueOnSuccess((FutureSuccessHandler<QueryResult, MySqlAsyncConnection>) result -> ComposableFutures.fromValue(MySqlAsyncConnection.this));
  }

  public ComposableFuture<QueryResult> commit() {
    return this.sendQuery("COMMIT;");
  }

  public ComposableFuture<QueryResult> rollBack() {
    return this.sendQuery("ROLLBACK;");
  }

  public ComposableFuture<MySqlAsyncConnection> disconnect() {
    final ComposableFuture<Connection> composableFuture = ScalaFutureHelper.from(conn::disconnect);

    return composableFuture.continueOnSuccess((SuccessHandler<Connection, MySqlAsyncConnection>) result -> {
      final MySQLConnection connection = (MySQLConnection) result;
      if (connection == conn) {
        return MySqlAsyncConnection.this;
      } else {
        return new MySqlAsyncConnection(connection);
      }
    });
  }

}
