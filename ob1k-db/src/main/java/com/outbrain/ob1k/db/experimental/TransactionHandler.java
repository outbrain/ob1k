package com.outbrain.ob1k.db.experimental;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * User: aronen
 * Date: 11/4/13
 * Time: 6:41 PM
 */
public interface TransactionHandler<T> {
  ComposableFuture<T> handle(MySqlAsyncConnection conn);
}
