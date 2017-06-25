package com.outbrain.ob1k.concurrent;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * @author marenzo
 * @since 2016-10-26
 */
public class TryTest {

  @Test
  public void creatingTry() {
    final Try<String> hello = Try.apply(() -> "hello");
    assertTrue("try is Success", hello.isSuccess());
  }

  @Test
  public void failureTry() {
    final Try<String> error = getError();
    assertTrue("try is Failure", error.isFailure());
  }

  @Test
  public void mappingValue() {
    final Try<String> hello = Try.apply(() -> "hello");
    final Try<Integer> digitOne = hello.map(__ -> 1);

    assertEquals("try is integer of one", (Object) 1, digitOne.getValue());
  }

  @Test
  public void flatMappingValue() {
    final Try<String> hello = Try.apply(() -> "hello");
    final Try<String> error = hello.flatMap(__ -> getError());

    assertTrue("try is Failure after mapping", error.isFailure());
  }

  @Test
  public void recovering() {
    final Try<String> error = getError();
    final Try<String> hello = error.recover(__ -> "hello");

    assertTrue("try is now containing 'hello' string", "hello".equals(hello.getValue()));
  }

  @Test
  public void recoveringFromSpecificError() {
    final Try<String> error = getError();
    final Try<String> hello = error.
      recover(IOException.class, __ -> "not this one").
      recover(OutOfMemoryError.class, __ -> "not even close").
      recover(RuntimeException.class, __ -> "hello");

    assertTrue("try is now containing 'hello' string", "hello".equals(hello.getValue()));
  }

  @Test
  public void recoveringWith() {
    final Try<String> error = getError();
    final Try<String> hello = error.recoverWith(__ -> Try.apply(() -> "hello"));

    assertTrue("try is now containing 'hello' string", "hello".equals(hello.getValue()));
  }

  @Test
  public void recoveringWithFromSpecificError() {
    final Try<String> error = getError();
    final Try<String> hello = error.
      recoverWith(IOException.class, __ -> Try.fromValue("not this one")).
      recoverWith(OutOfMemoryError.class, __ -> Try.fromValue("not even close")).
      recoverWith(RuntimeException.class, __ -> Try.fromValue("hello"));

    assertTrue("try is now containing 'hello' string", "hello".equals(hello.getValue()));
  }

  @Test
  public void ensureValue() {
    final Try<String> hello = Try.apply(() -> "hello");

    assertTrue("hello try containing String of 'hello'", hello.ensure("hello"::equals).isSuccess());
    assertTrue("hello try is not empty", hello.ensure(String::isEmpty).isFailure());
  }

  @Test
  public void consumeValue() {
    final AtomicReference<String> helloReference = new AtomicReference<>();
    final Try<String> hello = Try.apply(() -> "hello");

    hello.forEach(helloReference::set);

    assertTrue("reference is now set to 'hello' string", "hello".equals(helloReference.get()));
  }

  @Test
  public void transformTry() {
    final Try<String> hello = Try.apply(() -> "hello");
    final AtomicBoolean recoverNeverCalled = new AtomicBoolean(true);
    final Try<String> world = hello.transform(__ -> Try.apply(() -> "world"), __ -> {
      recoverNeverCalled.set(true);
      return getError();
    });

    assertTrue("world Try is containing 'world' string", "world".equals(world.getValue()));
    assertTrue("recover function never called", recoverNeverCalled.get());
  }

  @Test
  public void folding() {
    final Try<String> hello = Try.apply(() -> "hello");
    final Try<String> foldedValue = hello.fold(__ -> {
      throw new RuntimeException("I'm a bad person");
    }, __ -> "world");

    assertTrue("recovered Try to be 'world' string", "world".equals(foldedValue.getValue()));
  }
  
  @Test
  public void gettingValues() throws Throwable {
    final Try<String> hello = Try.apply(() -> "hello");

    assertTrue("hello try is 'hello' string", "hello".equals(hello.getValue()));
    assertNull("try is successful, getError should return null", hello.getError());
    assertTrue("'get' won't throw exception, because we have value", !hello.get().isEmpty());

    final Try<String> error = getError();

    assertTrue("'getOrElse' will return default value", "hello".equals(error.getOrElse(() -> "hello")));
    assertTrue("'orElse' will return successful try", error.orElse(() -> Try.apply(() -> "hello")).isSuccess());
  }
  
  @Test
  public void optionalOf() {
    assertFalse("optional of error is empty", getError().toOptional().isPresent());
    assertTrue("optional of success is present", Try.apply(() -> "hello").toOptional().isPresent());
  }

  private <T> Try<T> getError() {
    return Try.fromError(new RuntimeException("meh"));
  }
}
