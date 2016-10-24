package com.outbrain.ob1k.concurrent.scalaapi

import java.util
import java.util.concurrent.{Callable, TimeUnit, TimeoutException}
import java.util.function

import com.google.common.base.{Predicate, Supplier}
import com.outbrain.ob1k.concurrent.handlers._
import com.outbrain.ob1k.concurrent.{Consumer, Producer, ComposableFuture => JavaComposableFuture, ComposableFutures => JavaComposableFutures, Try => JavaTry}

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


/**
 * A Scala friendly adaptation of the ob1k ComposableFuture API.
 * Inspired by Scala's native Future API.
 *
 * @author slevin
 * @since 26/02/15
 */
object ComposableFuture {

  implicit def toScalaTry[T](javaTry: JavaTry[T]): Try[T] = {
    if (javaTry.isSuccess) Success(javaTry.getValue)
    else Failure(javaTry.getError)
  }

  implicit def toJavaTry[T](scalaTry: Try[T]): JavaTry[T] = {
    scalaTry match {
      case Success(value) => JavaTry.fromValue(value)
      case Failure(error) => JavaTry.fromError(error)
    }
  }

  implicit def toScalaComposableFuture[T](composableFuture: JavaComposableFuture[T]): ComposableFuture[T] =
    ComposableFuture(composableFuture)

  implicit class ComposableFutureExtensions[T](future: ComposableFuture[T]) {

    /**
     * Sets an initial timeout for this future, and retries it with further timeouts in case it fails due to a timeout.
     */
    def retryWithTimeouts(initialTimeout: Duration, moreTimeouts: Duration*): ComposableFuture[T] = {

      def tryHarder(chain: => ComposableFuture[T], timeouts: Duration*) = timeouts.toList match {
        case Nil =>
          chain
        case timeout :: rest =>
          chain.recoverWith({
            case e: Throwable
              if e.isInstanceOf[TimeoutException] ||
                Option(e.getCause).exists(_.isInstanceOf[TimeoutException]) =>
              future.timeoutAfter(timeout)
          })
      }

      tryHarder(future.timeoutAfter(initialTimeout), moreTimeouts: _*)
    }

    /**
     * Retries this future for a given amount of times in case it fails.
     */
    def retry(count: Int): ComposableFuture[T] = JavaComposableFutures.retry(count, new FutureAction[T] {
      override def execute(): JavaComposableFuture[T] = future.future
    })
  }

  private def apply[T](aFuture: JavaComposableFuture[T]): ComposableFuture[T] = new ComposableFuture[T] {
    override val future = aFuture
  }

  def fromValue[T](value: T): ComposableFuture[T] = JavaComposableFutures.fromValueLazy(value)

  def fromError[T](error: Throwable): ComposableFuture[T] = JavaComposableFutures.fromErrorLazy[T](error)

  def fromTry[T](aTry: Try[T]): ComposableFuture[T] = aTry match {
    case Success(s) => JavaComposableFutures.fromValueLazy[T](s)
    case Failure(e) => JavaComposableFutures.fromErrorLazy[T](e)
  }

  /**
   * Repeatedly chains futures until a predicate is satisfied.
   *
   * @param oracle A function that provides the futures to chain
   * @param predicate A predicate to satisfy
   * @tparam T the future type
   * @return A future which is the futures chained until the predicate was true.
   */
  def recursive[T](oracle: => ComposableFuture[T], predicate: T => Boolean): ComposableFuture[T] = {
    JavaComposableFutures.recursive(new Supplier[JavaComposableFuture[T]] {
      override def get(): JavaComposableFuture[T] = oracle.future
    }, new Predicate[T] {
      override def apply(input: T): Boolean = predicate(input)
    })
  }

  def all[T](failOnError: Boolean, futures: ComposableFuture[T]*): ComposableFuture[List[T]] = {
    val results: JavaComposableFuture[util.List[T]] = JavaComposableFutures.all(failOnError, futures.toList.map(_.future))
    results.map[List[T]](new function.Function[util.List[T], List[T]] {
      override def apply(result: util.List[T]): List[T] = {
        import scala.collection.JavaConverters._
        result.asScala.toList
      }
    })
  }

  /**
   * Combines futures' values into a single future which contains the result of the future completed first.
   *
   * @param futures futures to combine
   * @tparam T the common type param of all futures.
   * @return A combined future
   */
  def any[T](futures: ComposableFuture[T]*): ComposableFuture[T] = {
    JavaComposableFutures.any[T](futures.toList.map(_.future))
  }

  def first[K, T](futures: Map[K, ComposableFuture[T]], numOfSuccess: Int): ComposableFuture[Map[K, T]] = {
    val results: JavaComposableFuture[util.Map[K, T]] = JavaComposableFutures.first[K, T](futures.mapValues(cf => cf.future),
      numOfSuccess)

    results.map[Map[K, T]](new function.Function[util.Map[K, T], Map[K, T]] {
      override def apply(result: util.Map[K, T]): Map[K, T] = {
        import scala.collection.JavaConverters._
        result.asScala.toMap
      }
    })
  }

  /**
   * constructs a future that returns at most the first numOfSuccess successful values or whatever values
   * that was returned in the specified duration.
   * the input futures are given as a map so the response can indicate which of the futures actually returned.
   *
   * @param futures the input futures
   * @param numOfSuccess the min number of success values to wait for.
   * @param maxDuration the max time duration to wait for.
   * @tparam K the map key type
   * @tparam T the future type
   * @return a future of a map containing the accumulated results. 
   */
  def first[K, T](futures: Map[K, ComposableFuture[T]],
                  numOfSuccess: Int,
                  maxDuration: Duration): ComposableFuture[Map[K, T]] = {
    val results : ComposableFuture[util.Map[K, T]] = JavaComposableFutures.first[K, T](futures.mapValues(_.future),
                                      numOfSuccess,
                                      maxDuration.toNanos,
                                      TimeUnit.NANOSECONDS)

    results.map(new Function[util.Map[K, T], Map[K, T]] {
      override def apply(v1: util.Map[K, T]): Map[K, T] = {
        import scala.collection.JavaConverters._
        v1.asScala.toMap
      }
    })
  }

  def submit[T](task: => T, useExecutor: Boolean = true): ComposableFuture[T] = {
    JavaComposableFutures.submitLazy(useExecutor, new Callable[T] {
      override def call(): T = task
    })
  }

  /**
   * constructs a new ComposableFuture using a producer function.
   * producer can supply a value(or an error) to a consumer function(Try[T] => Unit)
   * and should supply it at-most once.
   *
   * @param producer the producer function.
   * @tparam T the type of the future
   * @return the constructed future.
   */
  def build[T](producer: (Try[T] => Unit) => Unit): ComposableFuture[T] = {
    JavaComposableFutures.buildLazy[T](new Producer[T] {
      override def produce(consumer: Consumer[T]): Unit = producer(consumer.consume(_))
    })
  }

  /**
   * Creates a future that holds the result of a delayed execution of a task.
   *
   * @param task The task to be executed by the future.
   * @param delay The delay with which to execute the task.
   * @tparam T the future type
   * @return A future that holds the result of task's execution.
   */
  def schedule[T](task: => T, delay: Duration): ComposableFuture[T] = {
    JavaComposableFutures.scheduleLazy(new Callable[T] {
      override def call(): T = task
    }, delay.toNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Creates a future that holds the result of a delayed execution of a computation.
   *
   * @param computation The computation to be executed by the future.
   * @param delay The delay with which to execute the computation.
   * @tparam T the future type
   * @return A future that holds the result of computation's execution.
   */
  def scheduleWith[T](computation: => ComposableFuture[T], delay: Duration): ComposableFuture[T] = {
    JavaComposableFutures.scheduleFuture(new Callable[JavaComposableFuture[T]] {
      override def call(): JavaComposableFuture[T] = computation.future
    }, delay.toNanos, TimeUnit.NANOSECONDS)
  }
}

trait ComposableFuture[T] {

  import com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture.{toScalaComposableFuture, toScalaTry}

  protected val future: JavaComposableFuture[T]

  def map[V](f: T => V): ComposableFuture[V] = future.map[V](new function.Function[T, V] {
    override def apply(result: T): V = f(result)
  })

  def flatMap[V](f: T => ComposableFuture[V]): ComposableFuture[V] = {
    future.flatMap[V](new function.Function[T, JavaComposableFuture[_ <: V]] {
      override def apply(result: T): JavaComposableFuture[V] = {
        Try(f(result)) match {
          case Success(ft) => ft.future
          case Failure(e) => ComposableFuture.fromError(e).future
        }
      }
    })
  }

  def consume(processResult: Try[T] => Unit): Unit = future.consume(new Consumer[T] {
    override def consume(result: JavaTry[T]): Unit = {
      processResult(result)
    }
  })


  def foreach(f: T => Unit): Unit = consume {_ foreach f}

  /**
   * Sets a timeout for getting a result from this future.
   * If the timeout value is Duration.Inf this method does nothing.
   */
  def timeoutAfter(duration: Duration): ComposableFuture[T] = duration match {
    case Duration.Inf => this
    case timeout => future.withTimeout(timeout.toNanos, TimeUnit.NANOSECONDS)
  }

  def recover(pf: PartialFunction[Throwable, T]): ComposableFuture[T] = future.recover(new function.Function[Throwable, T] {
    override def apply(error: Throwable): T = Try(pf(error)).orElse(Failure(error)).get
  })

  def recoverWith(pf: PartialFunction[Throwable, ComposableFuture[T]]): ComposableFuture[T] = {
    future.recoverWith(new function.Function[Throwable, JavaComposableFuture[_ <: T]] {
      override def apply(result: Throwable): JavaComposableFuture[T] = {
        Try(pf(result)).getOrElse(ComposableFuture.fromError(result)).future
      }
    })
  }

  def materialize(): ComposableFuture[T] = future.materialize()

  def get(atMost: Duration = Duration.Inf): Try[T] = atMost match {
    case Duration.Inf => Try(future.get())
    case duration => Try(future.withTimeout(atMost.toNanos, TimeUnit.NANOSECONDS).get())
  }

  def asJavaComposableFuture(): JavaComposableFuture[T] = future
}
