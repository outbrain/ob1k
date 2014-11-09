package com.outbrain.ob1k.db;

import java.util.concurrent.TimeUnit;

import com.github.mauricio.async.db.Configuration;
import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.mysql.MySQLConnection;
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory;
import com.github.mauricio.async.db.mysql.util.CharsetMapper;
import com.github.mauricio.async.db.util.ExecutorServiceUtils;
import com.github.mauricio.async.db.util.NettyUtils;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Created by aronen on 8/25/14.
 *
 * a dao for testing.
 * it configures the connection pool to use a single connection without auto commit.
 * all operations are performed on that single connection and when it is discarded the connection is rolled back
 */
public class BasicTestingDao extends BasicDao implements AutoCloseable {
  public BasicTestingDao(final String host, final int port, final String database, final String userName, final String password) throws Exception {
    // using a single connection in the pool so that every command will run on it.
      super(new MySqlConnectionPool(
            new NonCommittingSingleConnectionFactory(
                MySqlAsyncConnection.createConfiguration(host, port, database, userName, password)),
            1, null));
  }

  @Override
  public void close() throws Exception {
    execute("ROLLBACK;").continueWith(new FutureResultHandler<Long, Boolean>() {
      @Override
      public ComposableFuture<Boolean> handle(final ComposableFuture<Long> result) {
        return shutdown();
      }
    }).get();
  }

  private static class NonCommittingSingleConnectionFactory extends MySQLConnectionFactory {
    private final MySQLConnection connection;

    public NonCommittingSingleConnectionFactory(final Configuration configuration) throws Exception {
      super(configuration);

      connection = new MySQLConnection(configuration,
          CharsetMapper.Instance(),
          NettyUtils.DefaultEventLoopGroup(),
          ExecutorServiceUtils.CachedExecutionContext());

      Await.result(connection.connect(), Duration.apply(5, TimeUnit.SECONDS));
      final Future<QueryResult> futureRes = connection.sendQuery("SET autocommit=0;");
      Await.result(futureRes, Duration.apply(2, TimeUnit.SECONDS));
    }

    @Override
    public MySQLConnection create() {
      return connection;
    }
  }
}
