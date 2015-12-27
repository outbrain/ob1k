package com.outbrain.ob1k.http.marshalling;

import com.outbrain.ob1k.http.Response;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * HttpClient's Marshalling and Unmarshalling Strategy
 *
 * In cases of usage in typed execution methods in {@link com.outbrain.ob1k.http.RequestBuilder}
 * or in {@link com.outbrain.ob1k.http.RequestBuilder#setBody(Object)}
 * a strategy for marshalling and unmarshalling should be provided.
 *
 * @see JacksonMarshallingStrategy
 * @author marenzon
 */
public interface MarshallingStrategy {

  /**
   * The method that implements the unmarshalling logic
   *
   * @param type type of (T) to unmarshall
   * @param response response object
   * @param <T> type of object to unmarshall to
   * @return new instance of T, unmarshalled from the response
   * @throws IOException in case of failed attempt to unmarshall
   */
  <T> T unmarshall(final Type type, final Response response) throws IOException;

  /**
   * The method that implements the marshalling logic
   *
   * @param value object to marshall
   * @return byte[] of the marshalled value
   * @throws IOException in case of failed attempt to marshall
   */
  byte[] marshall(final Object value) throws IOException;
}