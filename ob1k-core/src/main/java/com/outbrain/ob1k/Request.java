package com.outbrain.ob1k;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * User: aronen
 * Date: 6/30/13
 * Time: 11:55 AM
 */
public interface Request {
  HttpRequestMethodType getMethod();

  String getUri();

  String getHeader(String name);

  List<String> getHeaders(String name);

  Map<String, String> getHeaders();

  Map<String, List<String>> getAllHeaders();

  long getContentLength();

  String getContentType();

  String getRequestBody();

  InputStream getRequestInputStream();

  InetSocketAddress getLocalAddress();

  InetSocketAddress getRemoteAddress();

  Map<String, String> getPathParams();

  String getPathParam(String key);

  String getQueryParam(String key);

  String getQueryParam(String key, String defaultValue);

  String getPostQueryParam(String key);

  String getPostQueryParam(String key, String defaultValue);

  List<String> getQueryParams(String key);

  Map<String, String> getQueryParams();

  String getPath();

  String getProtocol();

  String getContextPath();

  Map<String, String> getCookies();
}
