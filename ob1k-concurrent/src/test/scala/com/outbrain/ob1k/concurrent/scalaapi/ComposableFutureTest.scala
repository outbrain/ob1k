package com.outbrain.ob1k.concurrent.scalaapi

import java.util.concurrent.atomic.AtomicInteger

import com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.util.{Try, Success}

/**
 * Created by slevin on 2/27/15.
 *
 * test basic functionality
 */
class ComposableFutureTest extends FlatSpec {

  private def chainedFuturesWithFailure: ComposableFuture[String] = for {
    first <- schedule("a", 100 millis)
    second <- fromValue(first + "b")
    third <- fromValue(second + "c")
    forth <- submit(if (third == "abc") throw new scala.RuntimeException(third) else "hmm..?")
  } yield third

  "recursive" should "be called repeatedly until predicate is true" in {
    val atomicInt = new AtomicInteger
    val future = recursive[Int](fromValue(atomicInt.incrementAndGet), _ >= 10)
    future.get() shouldBe Success(10)
  }

  "Chained futures that generate an exception" should "generate FAILURE" in {
    chainedFuturesWithFailure.get() shouldBe 'failure
  }

  "Chained futures that generate an exception" should "generate SUCCESS if the chain is recovered" in {
    chainedFuturesWithFailure.recover({ case e: Throwable => "recovered!"}).get() shouldBe 'success
  }

  "Future with timeout" should "fail after timeout has elapsed" in {
    def oneWayTicket() = { val lock = new Object(); lock.synchronized(lock.wait()) }

    submit(oneWayTicket()).timeoutAfter(1 millis).get() shouldBe 'failure
  }

  "Retry with timeouts" should "succeed if any is sufficient, and fail if all exhausted" in {
    def wait() = Thread.sleep(5.milli.toMillis)

    submit(wait()).retryWithTimeouts(1 millis, 3 millis).get() shouldBe 'failure

    submit(wait()).retryWithTimeouts(2 millis, 10 seconds).get() shouldBe 'success
  }

  "build & map" should "succeed and return a mapped value" in {
    val future = build[Int](_(Success(1))).map(num => s"number #$num")
    future.get() shouldBe Success("number #1")
  }

  "take first 3 results" should "return the first 3 out of 4" in {
    val f1 = schedule(10, 10 millisecond)
    val f2 = schedule(30, 30 millisecond)
    val f3 = schedule(20, 20 millisecond)
    val f4 = schedule(15, 15 millisecond)

    val all = Map(1 -> f1, 2 -> f2, 3 -> f3, 4 -> f4)
    val res = first[Int, Int](all, 3, 40 millisecond)

    res.get() shouldBe Success(Map(1 -> 10, 4 -> 15, 3 -> 20))
  }

  "take first 3 results for given time" should "return the first 3 out of 4 in the given timeout" in {
    import scala.math.Ordering.Long

    val f1 = schedule(10, 10 millisecond)
    val f2 = schedule(30, 100 millisecond)
    val f3 = schedule(20, 20 millisecond)
    val f4 = schedule(15, 15 millisecond)

    val all = Map(1 -> f1, 2 -> f2, 3 -> f3, 4 -> f4)
    val res = first[Int, Int](all, 4, 40 millisecond)

    val t1 = System.currentTimeMillis()
    val resMap: Try[Map[Int, Int]] = res.get()
    val totalTime: Long = System.currentTimeMillis() - t1

    resMap shouldBe Success(Map(1 -> 10, 4 -> 15, 3 -> 20))
    totalTime should be < 50L
  }

}
