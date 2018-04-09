package com.outbrain.ob1k.server.netty;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * User: aronen
 * Date: 6/30/13
 * Time: 12:02 PM
 */
public class NettyRequest implements Request {

  private final HttpRequest inner;
  private final Channel channel;
  private final QueryStringDecoder getQueryDecoder;
  private final HttpContent content;
  private final String contextPath;
  private final Map<String, String> pathParams;
  private volatile QueryStringDecoder postQueryDecoder;
  private volatile Map<String, Cookie> cookies;

  NettyRequest(final HttpRequest inner,
               final HttpContent content,
               final Channel channel,
               final String contextPath) {
    this.inner = inner;
    this.content = content;
    this.channel = channel;
    this.getQueryDecoder = new QueryStringDecoder(inner.getUri());
    this.contextPath = contextPath;
    this.pathParams = new HashMap<>();
  }

  @Override
  public HttpRequestMethodType getMethod() {
    return HttpRequestMethodType.valueOf(inner.getMethod().name().toUpperCase());
  }

  @Override
  public String getUri() {
    return inner.getUri();
  }

  @Override
  public String getHeader(final String name) {
    return inner.headers().get(name);
  }

  @Override
  public List<String> getHeaders(final String name) {
    return inner.headers().getAll(name);
  }

  @Override
  public Map<String, String> getHeaders() {
    final HttpHeaders headers = inner.headers();
    final List<Map.Entry<String, String>> entries = headers.entries();
    final Map<String, String> result = new HashMap<>();
    for (final Map.Entry<String, String> entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }

    return result;
  }

  @Override
  public Map<String, List<String>> getAllHeaders() {
    final HttpHeaders headers = inner.headers();
    final List<Map.Entry<String, String>> entries = headers.entries();
    final Map<String, List<String>> result = new HashMap<>();

    for (final Map.Entry<String, String> entry : entries) {
      final List<String> list = result.computeIfAbsent(entry.getKey(), __ -> new ArrayList<>());
      list.add(entry.getValue());
    }

    return result;
  }

  @Override
  public long getContentLength() {
    return content.content().readableBytes();
  }

  @Override
  public String getContentType() {
    return inner.headers().get(CONTENT_TYPE);
  }

  @Override
  public String getRequestBody() {
    final ByteBuf buffer = content.content();
    return buffer.toString(CharsetUtil.UTF_8);
  }

  @Override
  public InputStream getRequestInputStream() {
    return new ByteBufInputStream(content.content());
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public Map<String, String> getPathParams() {
    return pathParams;
  }

  @Override
  public String getPathParam(final String key) {
    return pathParams.get(key);
  }

  @Override
  public String getQueryParam(final String key) {
    final Map<String, List<String>> parameters = getQueryDecoder.parameters();
    final List<String> valueOptions = parameters.get(key);
    return  (valueOptions == null || valueOptions.isEmpty()) ? null : valueOptions.get(0);
  }

  @Override
  public String getPostQueryParam(final String key) {
    if (postQueryDecoder == null) {
      postQueryDecoder = new QueryStringDecoder(getRequestBody(), false);
    }

    final Map<String, List<String>> parameters = postQueryDecoder.parameters();
    final List<String> valueOptions = parameters.get(key);
    return  (valueOptions == null || valueOptions.isEmpty()) ? null : valueOptions.get(0);
  }

  @Override
  public String getPostQueryParam(final String key, final String defaultValue) {
    final String res = getPostQueryParam(key);
    return res != null ? res : defaultValue;
  }

  @Override
  public String getQueryParam(final String key, final String defaultValue) {
    final String res = getQueryParam(key);
    return res != null ? res : defaultValue;
  }

  @Override
  public List<String> getQueryParams(final String key) {
    final Map<String, List<String>> parameters = getQueryDecoder.parameters();
    return parameters.get(key);
  }

  @Override
  public Map<String, String> getQueryParams() {
    final Map<String, List<String>> parameters = getQueryDecoder.parameters();
    final Map<String, String> result = new HashMap<>();
    for (final String key : parameters.keySet()) {
      result.put(key, parameters.get(key).get(0));
    }

    return result;
  }

  @Override
  public String getPath() {
    return getQueryDecoder.path();
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public String getCookie(final String cookieName) {
    if (cookies == null) {
      populateCookies();
    }

    final Cookie cookie = cookies.get(cookieName);
    return cookie != null ? cookie.getValue() : null;
  }

  private void populateCookies() {
    cookies = Maps.newHashMap();

    final String cookieHeaderValue = inner.headers().get("Cookie");
    if (cookieHeaderValue == null) return;

    final Set<Cookie> cookiesSet = CookieDecoder.decode(cookieHeaderValue, false);

    for (final Cookie cookie : cookiesSet) {
      cookies.put(cookie.getName(), cookie);
    }
  }

  @Override
  public String getProtocol() {
    return inner.getProtocolVersion().text();
  }
}
