package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.ResultHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * @author Eran Harel
 */
public class JsonTranscoderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonTranscoder<Person> transcoder = new JsonTranscoder<>(objectMapper, Person.class);

  @Test
  public void testRoundTripTranscoding() {
    final Person person = new Person(66, "Saba", 90);
    final Person decodedPerson = transcoder.decode(transcoder.encode(person));
    Assert.assertEquals("decoded person age", person.age, decodedPerson.age);
    Assert.assertEquals("decoded person name", person.name, decodedPerson.name);
    Assert.assertEquals("decoded person weight", person.weight, decodedPerson.weight, 0.01f);
    Assert.assertNotSame(person, decodedPerson);
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    final Logger logger = LoggerFactory.getLogger(JsonTranscoderTest.class);
    ComposableFutures.fromNull().continueOnSuccess(new SuccessHandler<Object, Object>() {
      @Override
      public Object handle(Object result) throws ExecutionException {
        throw new OutOfMemoryError();
      }
    }).continueOnSuccess(new SuccessHandler<Object, Object>() {
      @Override
      public Object handle(final Object o) throws ExecutionException {
        logger.warn("success handler");
        return null;
      }
    }).continueOnError(new ErrorHandler<Object>() {
      @Override
      public Object handle(final Throwable throwable) throws ExecutionException {
        logger.warn("error handler");
        return null;
      }
    }).continueWith(new ResultHandler<Object, Object>() {
      @Override
      public Object handle(final Try<Object> aTry) throws ExecutionException {
        logger.warn("result handler");
        return null;
      }
    }).get();
//    ComposableFutures.fromNull().continueOnSuccess(new SuccessHandler<Object, Object>() {
//      @Override
//      public Object handle(Object result) throws ExecutionException {
//        throw new OutOfMemoryError();
//      }
//    }).get();
//
//    System.out.println("MEH");
  }

}
