package com.outbrain.ob1k.db;

import java.util.concurrent.ExecutionException;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;

/**
 * Created by aronen on 8/25/14.
 *
 * a dao for testing.
 * it configures the connection pool to use a single connection without auto commit.
 * all operations are performed on that single connection and when it is discarded the connection is rolled back
 */
public class BasicTestingDao extends BasicDao implements AutoCloseable {
  public BasicTestingDao(final String host, final int port, final String database, final String userName, final String password) {
    // using a single connection in the pool so that every command will run on it.
    super(host, port, database, userName, password, 1, null);
    try {
      execute("SET autocommit=0;").get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("can't set the connection to auto commit false mode.", e);
    }
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
}
