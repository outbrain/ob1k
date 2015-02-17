package com.outbrain.ob1k.concurrent.eager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: aronen
 * Date: 6/17/13
 * Time: 6:42 PM
 */
public class HandlersList {
  private final AtomicReference<List<Runnable>> handlers;

  public HandlersList() {
    this.handlers = new AtomicReference<List<Runnable>>(new ArrayList<Runnable>());
  }

  public void addHandler(final Runnable handler, final Executor executor) {
    while (true) {
      final List<Runnable> list = handlers.get();
      if (list == null) {
        if (executor != null) {
          executor.execute(handler);
        } else {
          handler.run();
        }
        return;
      }

      final List<Runnable> newList = new ArrayList<>(list);
      newList.add(handler);

      final boolean success = handlers.compareAndSet(list, newList);
      if (success) {
        return;
      }
    }
  }

  public void execute(final Executor executor) {
    while (true) {
      final List<Runnable> list = handlers.get();
      if (list == null) {
        return;
      }

      final boolean success = handlers.compareAndSet(list, null);
      if (success) {
        for (final Runnable task : list) {
          if (executor != null) {
            executor.execute(task);
          } else {
            task.run();
          }
        }
        return;
      }
    }
  }


}
