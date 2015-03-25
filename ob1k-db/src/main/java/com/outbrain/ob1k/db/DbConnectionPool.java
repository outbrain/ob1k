package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.QueryResult;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.List;

/**
 * Created by eran on 3/25/15.
 */
public interface DbConnectionPool {
  ComposableFuture<QueryResult> sendQuery(String query);

  ComposableFuture<QueryResult> sendPreparedStatement(String query, List<Object> values);

  <T> ComposableFuture<T> withConnection(TransactionHandler<T> handler);

  <T> ComposableFuture<T> withTransaction(TransactionHandler<T> handler);

  ComposableFuture<Boolean> close();
}
