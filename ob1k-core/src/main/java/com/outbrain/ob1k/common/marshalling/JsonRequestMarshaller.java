package com.outbrain.ob1k.common.marshalling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.outbrain.ob1k.http.common.ContentType.JSON;

/**
 * @author marenzon
 */
public class JsonRequestMarshaller extends JacksonRequestMarshaller {

  public JsonRequestMarshaller() {
    super(createObjectMapper(), JSON);
  }

  @Override
  public byte[] marshallRequestParams(final Object[] requestParams) throws IOException {
    // requests can come from a regular httpClient post request with a single param that get wrapped inside an array
    // or in case of a real RPC call with a single param. in both cases we unwrap it and send it as is.
    // the code in unmarshallRequestParams() know how to deal with both single object or array of objects.
    final Object params = requestParams == null ? new Object[0] :
      requestParams.length == 1 ? requestParams[0] : requestParams;

    return marshallingStrategy.marshall(params);
  }

  private static ObjectMapper createObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper;
  }
}