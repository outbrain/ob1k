# ComposableFuture - Asynchronous Computation Representation

## Introduction
[ComposableFuture](https://github.com/outbrain/ob1k/blob/master/ob1k-concurrent/src/main/java/com/outbrain/ob1k/concurrent/ComposableFuture.java) is a monadic construct that represents (asynchronous) computation of task resulted
in value or error. The purpose of such construct is made to provide simple paradigm of 
composing and encapsulating concurrent operations and their synchronization.

ComposableFuture uses Producer/Consumer way to create computation task,
where each task produces the final value and consumed to the future.

ComposableFuture comes in two manners - Eager and Lazy:  
**Eager** future represents computation task that its start point of execution is not controlled by the user -
once eager future is created, the task is guaranteed to run at some point of time.  
**Lazy** future represents computation task, where the producer will start run only upon consume request.  
Also, compared to Eager, it is stateless and upon each consume call the whole chain will be executed.

Default future type is Eager.

ComposableFuture is not subtype of [j.u.c.Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), but an independent
implementation providing composition interface similar to other Future implementations, such as [Scala's](http://www.scala-lang.org/api/2.12.x/scala/concurrent/Future.html).

[Ob1k](https://github.com/outbrain/ob1k) uses ComposableFuture to encapsulate asynchronous I/O operations such as RPCs.

## Few examples
To create a future for our computation task, we'll use [ComposableFutures](https://github.com/outbrain/ob1k/blob/master/ob1k-concurrent/src/main/java/com/outbrain/ob1k/concurrent/ComposableFutures.java) `build` method to write our producer:

```java
ComposableFutures.build(producer -> {
  producer.consume(Try.fromValue("hello"));
});
```

As each computation may finish with either value or error, we're using [Try](https://github.com/outbrain/ob1k/blob/master/ob1k-concurrent/src/main/java/com/outbrain/ob1k/concurrent/Try.java) construct to represent the result.  
Once we did that, now we can start compose our future into sequence of operations:
```java
ComposableFuture<String> futureValue = fetchNameFromNetwork(); // using the future 'build' returned
ComposableFuture<String> mappedValue = futureValue.map(value -> value + " world");
mappedValue.consume(resultTry -> {
    System.out.println(resultTry); // "Success("Hello World");"
});
```

And that was our first composition!

Take a look at our [code examples](https://github.com/outbrain/ob1k/tree/cf_new_api/ob1k-concurrent/src/test/java/com/outbrain/ob1k/concurrent/examples) to see which helpers and best practices should be used with ComposableFuture.

## Helpers
Besides ComposableFuture, you can find an implementation of [Try](https://github.com/outbrain/ob1k/blob/master/ob1k-concurrent/src/main/java/com/outbrain/ob1k/concurrent/Try.java) also in this package.  
Try is a simple construct represents **computed** value - that can either Failure or Success.  
Instead of dealing with plain old try/catch statements, you can use Try type to compose operations on top of the result in a functional way.

## FAQ
1. May I throw exceptions in mappers?  
If you're calling a method with checked exception, you can use 'flatMap' method and return failed ComposableFuture with the exception (`ComposableFutures#fromError`),
or you may throw any runtime exception wrapping that one (which internally will be translated to failed ComposableFuture).
If you decided throwing runtime exception, you can wrap your exception with [UncheckedExecutionException](https://github.com/outbrain/ob1k/blob/cf_new_api/ob1k-concurrent/src/main/java/com/outbrain/ob1k/concurrent/UncheckedExecutionException.java),
where internally the code will unwrap this boxing and place the actual error as the failure exception.