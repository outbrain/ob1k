package com.outbrain.ob1k.common.marshalling;

import com.ning.http.client.Response;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by gmarom on 6/25/15
 */
public class JsonRequestMarshallerTest {

  private JsonRequestMarshaller jsonRequestMarshaller;

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    jsonRequestMarshaller = new JsonRequestMarshaller();
  }

  @After
  public void tearDown() throws Exception {
    jsonRequestMarshaller = null;
  }

  @Test
  public void testUnmarshallNoContent() throws IOException {
    final Response response = mockResponse(HttpResponseStatus.NO_CONTENT.code(), null);
    assertNull(jsonRequestMarshaller.unmarshallResponse(response, TestBody.class, true));
  }

  @Test
  public void testUnmarshalWithOK() throws IOException {
    final String body = "{\"prop\": \"test\"}";
    final Response response = mockResponse(HttpResponseStatus.OK.code(), body);
    final TestBody result = (TestBody) jsonRequestMarshaller.unmarshallResponse(response, TestBody.class, true);
    assertEquals("test", result.getProp());
  }

  @Test
  public void testUnmarshalWithErrorWithoutFail() throws IOException {
    final String body = "{\"prop\": \"test\"}";
    final Response response = mockResponse(HttpResponseStatus.NOT_FOUND.code(), body);
    final TestBody result = (TestBody) jsonRequestMarshaller.unmarshallResponse(response, TestBody.class, false);
    assertEquals("test", result.getProp());
  }

  @Test
  public void testUnmarshalWithError() throws IOException {
    expectedException.expect(IOException.class);

    final Response response = mockResponse(HttpResponseStatus.NOT_FOUND.code(), null);
    jsonRequestMarshaller.unmarshallResponse(response, TestBody.class, true);
  }

  private Response mockResponse(final int statusCode, final String body) throws IOException {
    final Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(statusCode);
    when(response.getResponseBody()).thenReturn(body);
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