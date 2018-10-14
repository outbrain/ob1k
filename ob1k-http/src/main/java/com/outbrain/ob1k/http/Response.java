package com.outbrain.ob1k.http;

import com.outbrain.ob1k.http.common.Cookie;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents the http response of request execution

 * @see RequestBuilder
 * @author marenzon
 */
public interface Response {

  int getStatusCode();

  String getStatusText();

  URI getUri() throws URISyntaxException;

  String getUrl();

  String getContentType();

  byte[] getResponseBodyAsBytes() throws IOException;

  InputStream getResponseBodyAsStream() throws IOException;

  ByteBuffer getResponseBodyAsByteBuffer() throws IOException;

  List<Cookie> getCookies();

  String getResponseBody() throws IOException;

  String getHeader(String name);

  List<String> getHeaders(String name);

  Iterator<Map.Entry<String, String>> getHeaders();

  boolean isRedirected();

  boolean hasResponseBody();

  boolean hasResponseStatus();

  boolean hasResponseHeaders();
}
