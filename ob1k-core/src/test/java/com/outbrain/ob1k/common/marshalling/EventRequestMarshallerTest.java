package com.outbrain.ob1k.common.marshalling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbrain.ob1k.http.Response;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class EventRequestMarshallerTest {

  private EventRequestMarshaller eventRequestMarshaller;
  private final ObjectMapper om = new ObjectMapper();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    eventRequestMarshaller = new EventRequestMarshaller();
  }

  @After
  public void tearDown() throws Exception {
    eventRequestMarshaller = null;
  }

  @Test
  public void testUnmarshallNoContent() throws IOException {
    final Response response = mockResponse(HttpResponseStatus.NO_CONTENT.code(), null);
    assertNull(eventRequestMarshaller.unmarshallResponse(response, TestBody.class));
  }

  @Test
  public void testUnmarshalWithOK() throws IOException {
    final String body = "{\"prop\": \"test\"}";

    final Response response = mockResponse(HttpResponseStatus.OK.code(), body);
    final TestBody result = eventRequestMarshaller.unmarshallResponse(response, TestBody.class);
    assertEquals("test", result.getProp());
  }


  @Test
  public void testmarshalStreamWithOK() throws IOException {

    final String body = "{\"prop\":\"test\"}";
    final TestBody testBody = om.readValue(body, TestBody.class);

    final HttpContent result = eventRequestMarshaller.marshallResponsePart(testBody, HttpResponseStatus.OK, true);
    final ByteBuf jsonBuf = result.content();
    final String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8);

    assertNotNull("response shouldn't be null", result);
    assertEquals("data not equals", "data: " + body + "\n\n", jsonStr);
  }


  @Test
  public void testUnmarshalWithError() throws IOException {
    expectedException.expect(IOException.class);
    final Response response = mockResponse(HttpResponseStatus.NOT_FOUND.code(), null);
    eventRequestMarshaller.unmarshallResponse(response, TestBody.class);
  }

  private Response mockResponse(final int statusCode, final String body) throws IOException {
    final Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(statusCode);
    when(response.getResponseBody()).thenReturn(body);
    when(response.getResponseBodyAsBytes()).thenReturn(body == null ? null : body.getBytes());
    when(response.hasResponseBody()).thenReturn(true);
    return response;
  }

  public static class TestBody {
    String prop;

    public String getProp() {
      return prop;
    }

    @SuppressWarnings("unused")
    public void setProp(final String prop) {
      this.prop = prop;
    }
  }

}
