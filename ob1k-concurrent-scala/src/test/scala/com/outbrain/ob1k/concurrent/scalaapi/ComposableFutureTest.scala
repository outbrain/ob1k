package com.outbrain.ob1k.concurrent.scalaapi

import java.util.concurrent.atomic.AtomicInteger

import com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture._
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by slevin on 2/27/15.
 */
@RunWith(classOf[JUnitRunner])
class ComposableFutureTest extends FlatSpec {

  private def chainedFuturesWithFailure: ComposableFuture[String] = for {
    first <- ComposableFuture.schedule("a", 100 millis)
    second <- ComposableFuture.fromValue(first + "b")
    third <- ComposableFuture.fromValue(second + "c")
    forth <- ComposableFuture.submit(if (third == "abc") throw new scala.RuntimeException(third) else "hmm..?")
  } yield third

  "recursive" should "be called repeatedly until predicate is true" in {
    val atomicInt = new AtomicInteger
    val future = ComposableFuture.recursive[Int](ComposableFuture.fromValue(atomicInt.incrementAndGet), _ >= 10)
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

    ComposableFuture.submit(oneWayTicket()).timeoutAfter(1 millis).get() shouldBe 'failure
  }

  "Retry with timeouts" should "succeed if any is sufficient, and fail if all exhausted" in {
    def wait() = Thread.sleep(5.milli.toMillis)

    ComposableFuture.submit(wait()).retryWithTimeouts(1 millis, 3 millis).get() shouldBe 'failure

    ComposableFuture.submit(wait()).retryWithTimeouts(2 millis, 10 seconds).get() shouldBe 'success
  }
}
