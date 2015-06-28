package com.outbrain.ob1k.http;

import com.outbrain.ob1k.http.common.Cookie;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

  String getContentType();

  byte[] getResponseBodyAsBytes() throws IOException;

  InputStream getResponseBodyAsStream() throws IOException;

  List<Cookie> getCookies();

  String getResponseBody() throws IOException;

  String getHeader(String name);

  List<String> getHeaders(String name);

  Map<String, List<String>> getHeaders();

  boolean isRedirected();
}