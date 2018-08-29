package com.outbrain.ob1k.db.experimental;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.SSLConfiguration;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


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

  public static Configuration createConfiguration(final String host, final int port, final Optional<String> database, final String userName, final Optional<String> password,
                                                  final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) {
    final Optional<Duration> queryTimeout = queryTimeoutMilliSeconds == -1 ?
      Optional.empty() :
      Optional.of(Duration.ofMillis(queryTimeoutMilliSeconds));

    return new Configuration(userName, host, port, password.orElse(null), database.orElse(null), new SSLConfiguration(), CharsetUtil.UTF_8, MAXIMUM_MESSAGE_SIZE,
      PooledByteBufAllocator.DEFAULT, Duration.ofMillis(connectTimeoutMilliSeconds), Duration.ofSeconds(4),
      queryTimeout.orElse(null));
  }

  MySQLConnection getInnerConnection() {
    return conn;
  }

  public ComposableFuture<QueryResult> sendQuery(final String query) {
    return JavaFutureHelper.from(() -> conn.sendQuery(query));
  }

  public ComposableFuture<QueryResult> sendPreparedStatement(final String query, final List<Object> values) {
    return JavaFutureHelper.from(() -> conn.sendPreparedStatement(query, values));
  }

  public ComposableFuture<MySqlAsyncConnection> connect() {
    if (conn.isConnected()) {
      return ComposableFutures.fromValue(this);
    }

    final ComposableFuture<Connection> composableFuture = JavaFutureHelper.from(conn::connect);

    return composableFuture.map(result -> {
      final MySQLConnection connection = (MySQLConnection) result;
      if (connection == conn) {
        return MySqlAsyncConnection.this;
      } else {
        return new MySqlAsyncConnection(connection);
      }
    });
  }

  public ComposableFuture<MySqlAsyncConnection> startTx() {
    return this.sendQuery("START TRANSACTION;").flatMap(result -> ComposableFutures.fromValue(MySqlAsyncConnection.this));
  }

  public ComposableFuture<QueryResult> commit() {
    return this.sendQuery("COMMIT;");
  }

  public ComposableFuture<QueryResult> rollBack() {
    return this.sendQuery("ROLLBACK;");
  }

  public ComposableFuture<MySqlAsyncConnection> disconnect() {
    final ComposableFuture<Connection> composableFuture = JavaFutureHelper.from(conn::disconnect);

    return composableFuture.map(result -> {
      final MySQLConnection connection = (MySQLConnection) result;
      if (connection == conn) {
        return MySqlAsyncConnection.this;
      } else {
        return new MySqlAsyncConnection(connection);
      }
    });
  }

}
