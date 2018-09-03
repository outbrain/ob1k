package com.outbrain.ob1k.db.experimental;


import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.exceptions.ConnectionNotConnectedException;
import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory;
import com.github.jasync.sql.db.util.Try;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * a dao for testing.
 * it configures the connection pool to use a single connection without auto commit.
 * all operations are performed on that single connection and when it is discarded the connection is rolled back
 *
 * @author aronen 8/25/14.
 */
public class BasicTestingDao extends BasicDao implements AutoCloseable {

  public BasicTestingDao(final String host, final int port, final String database, final String userName, final String password,
                         final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) throws Exception {
    // using a single connection in the pool so that every command will run on it.
    super(createTestConnectionPool(host, port, database, userName, password, connectTimeoutMilliSeconds, queryTimeoutMilliSeconds));
  }

  private static DbConnectionPool createTestConnectionPool(final String host, final int port, final String database, final String userName, final String password,
                                                           final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) throws Exception {
    return MySqlConnectionPoolBuilder.newBuilder(
      createNonCommittingSingleConnectionFactory(host, port, database, userName, password, connectTimeoutMilliSeconds, queryTimeoutMilliSeconds)
    ).maxConnections(1).build();
  }

  private static NonCommittingSingleConnectionFactory createNonCommittingSingleConnectionFactory(final String host, final int port, final String database, final String userName, final String password,
                                                                                                 final long connectTimeoutMilliSeconds, final long queryTimeoutMilliSeconds) throws Exception {
    return new NonCommittingSingleConnectionFactory(
      MySqlAsyncConnection.createConfiguration(host, port, Optional.ofNullable(database), userName, Optional.ofNullable(password),
        connectTimeoutMilliSeconds, queryTimeoutMilliSeconds));
  }

  @Override
  public <T> ComposableFuture<T> withTransaction(final TransactionHandler<T> handler) {
    return withConnection(handler);
  }

  @Override
  public void close() throws Exception {
    execute("rollback;").alwaysWith(result -> shutdown()).get();
  }

  private static class NonCommittingSingleConnectionFactory extends MySQLConnectionFactory {
    private int numOfCreations = 0;
    private final Configuration configuration;
    public NonCommittingSingleConnectionFactory(final Configuration configuration) throws Exception {
      super(configuration);
      this.configuration = configuration;
    }

    @Override
    public MySQLConnection create() {
      numOfCreations++;
      if (numOfCreations > 1) {
        throw new IllegalStateException("trying to create more that one connection per test");
      }

      final MySQLConnection conn = super.create();
      final Future<QueryResult> futureRes = conn.sendQuery("start transaction;");
      try {
        futureRes.get(configuration.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS);
        return conn;
      } catch (final Exception e) {
        throw new IllegalStateException("can't start transaction on the connection", e);
      }
    }

    @Override
    public Try<MySQLConnection> validate(final MySQLConnection item) {
      if (!item.isConnected()) {
        return new Try.Failure(new ConnectionNotConnectedException(item));
      }

      if (item.isQuerying()) {
        return new Try.Failure(new ConnectionStillRunningQueryException(item.count(), false));
      }

      return new Try.Success<>(item);
    }
  }
}
