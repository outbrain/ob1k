package com.outbrain.ob1k.crud

import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures

internal fun <T> List<ComposableFuture<T>>.all(failOnError: Boolean = true) = ComposableFutures.all(failOnError, this)
internal fun <T, X> List<ComposableFuture<T>>.mapAll(failOnError: Boolean = true, mapper: (List<T>) -> X) = all(failOnError).map(mapper)
internal fun <T> T.asFuture() = ComposableFutures.fromValue(this)