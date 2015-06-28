package com.outbrain.ob1k.http.marshalling;

import com.outbrain.ob1k.http.Response;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author marenzon
 */
public interface UnmarshallingStrategy {

  <T> T unmarshall(final Type type, final Response response) throws IOException;
}