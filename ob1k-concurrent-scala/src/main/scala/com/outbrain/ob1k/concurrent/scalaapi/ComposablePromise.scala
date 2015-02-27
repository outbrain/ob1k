package com.outbrain.ob1k.concurrent.scalaapi

import java.util.concurrent.Executor

import com.outbrain.ob1k.concurrent.eager.{ComposablePromise => JavaComposablePromise}
import com.outbrain.ob1k.concurrent.{ComposableFutures => JavaComposableFutures}

import scala.util.{Failure, Success, Try}

/**
 * A Scala friendly adaptation of the ob1k ComposablePromise API.
 * Inspired by Scala's native Future API.
 *
 * @author slevin
 * @since 26/02/15
 */
object ComposablePromise {

  implicit def toScalaComposablePromise[T](composablePromise: JavaComposablePromise[T]): ComposablePromise[T] = {
    ComposablePromise(composablePromise)
  }

  def apply[T](): ComposablePromise[T] = {
    JavaComposableFutures.newPromise[T]()
  }

  def apply[T](executor: Executor): ComposablePromise[T] = {
    JavaComposableFutures.newPromise[T](executor)
  }

  private def apply[T](composablePromise: JavaComposablePromise[T]): ComposablePromise[T] = {
    new ComposablePromise[T] {
      override val promise = composablePromise
    }
  }
}

trait ComposablePromise[T] {

  protected val promise: JavaComposablePromise[T]

  /** Completes the promise with either an exception or a value.
    *
    * @param tryValue     Either the value or the exception to complete the promise with.
    *
    */
  def complete(tryValue: Try[T]): this.type = {
    tryValue match {
      case Success(v) => promise.set(v)
      case Failure(e) => promise.setException(e)
    }

    this
  }

  /** Completes this promise with the specified future, once that future is completed.
    *
    * @return   This promise
    */
  final def completeWith(other: ComposableFuture[T]): this.type = {
    other consume complete
    this
  }

  /** Completes the promise with either an exception or a value.
    *
    * @param e     Either the value or the exception to complete the promise with.
    */
  def failure(e: Throwable): this.type = complete(Failure(e))

  /** Completes the promise with a value.
    *
    * @param v    The value to complete the promise with.
    *
    */
  def success(v: T): this.type = complete(Success(v))

  /**
   * Returns the future containing the value of this promise.
   */
  def future: ComposableFuture[T] = promise.future()
}
