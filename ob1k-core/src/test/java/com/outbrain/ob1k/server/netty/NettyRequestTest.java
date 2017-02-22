package com.outbrain.ob1k.server.netty;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author guymarom
 */
public class NettyRequestTest {

  private final static String COOKIE_NAME = "test_cookie_name";
  private final static String COOKIE_VALUE = "cookie_value";
  public static final String COOKIE_HEADER_NAME = "Cookie";

  private HttpHeaders httpHeaders;
  private NettyRequest request;

  @Before
  public void setup() {
    httpHeaders = new DefaultHttpHeaders();

    final HttpRequest innerRequest = mock(HttpRequest.class);
    when(innerRequest.uri()).thenReturn("http://localhost:8080");
    when(innerRequest.headers()).thenReturn(httpHeaders);

    final HttpContent httpContent = mock(HttpContent.class);
    final Channel channel = mock(Channel.class);

    request = new NettyRequest(innerRequest, httpContent, channel, "/");
  }

  @After
  public void teardown() {
    httpHeaders = null;
    request = null;
  }

  //Tests the case where a cookie is fetched when no "Cookie" header is present in the request
  @Test
  public void testGetCookieNoHeader() {
    assertNull(request.getCookie(COOKIE_NAME));
  }

  @Test
  public void testCookieEmptyHeader() {
    httpHeaders.add(COOKIE_HEADER_NAME, "");
    assertNull(request.getCookie(COOKIE_NAME));
  }

  @Test
  public void testGetValidCookie() {
    httpHeaders.add(COOKIE_HEADER_NAME, COOKIE_NAME + "=" + COOKIE_VALUE);
    assertEquals(COOKIE_VALUE, request.getCookie(COOKIE_NAME));
  }

}