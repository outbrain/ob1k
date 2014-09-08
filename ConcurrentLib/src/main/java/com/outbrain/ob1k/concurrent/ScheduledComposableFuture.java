package com.outbrain.ob1k.concurrent;

import java.util.concurrent.ScheduledFuture;

/**
 * User: aronen
 * Date: 10/2/13
 * Time: 12:35 AM
 */
public interface ScheduledComposableFuture<T> extends ComposableFuture<T>, ScheduledFuture<T> {
}
