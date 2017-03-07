package com.outbrain.ob1k.server.netty;

import com.outbrain.ob1k.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

/**
 * Builds a response, and allows one to add headers and content.
 *
 * @author Eran Harel
 */
public class ResponseBuilder {

  private HttpResponseStatus status;
  private ByteBuf rawContent;
  private HttpHeaders headers;
  private Object message;

  public static ResponseBuilder ok() {
    return fromStatus(HttpResponseStatus.OK);
  }

  public static ResponseBuilder fromStatus(final int status) {
    return new ResponseBuilder().withStatus(status);
  }

  public static ResponseBuilder fromStatus(final HttpResponseStatus status) {
    return new ResponseBuilder().withStatus(status);
  }

  private ResponseBuilder withStatus(final int status) {
    return withStatus(HttpResponseStatus.valueOf(status));
  }

  // TODO do we want this API coupling to Netty?
  private ResponseBuilder withStatus(final HttpResponseStatus status) {
    if (null == status) {
      throw new IllegalArgumentException("Status must not be null");
    }

    this.status = status;
    return this;
  }

  public ResponseBuilder withMessage(final Object message) {
    this.message = message;
    return this;
  }

  public ResponseBuilder withContent(final CharSequence rawContent) {
    return withContent(rawContent, CharsetUtil.UTF_8);
  }

  public ResponseBuilder withContent(final CharSequence rawContent, final Charset charset) {
    return withContent(Unpooled.copiedBuffer(rawContent, charset));
  }

  public ResponseBuilder withContent(final byte[] rawContent) {
    return withContent(Unpooled.copiedBuffer(rawContent));
  }

  // TODO do we want this API coupling to Netty?
  public ResponseBuilder withContent(final ByteBuf rawContent) {
    this.rawContent = rawContent;
    return this;
  }

  // TODO do we want this API coupling to Netty?
  public ResponseBuilder withHeaders(final HttpHeaders headers) {
    this.headers = headers;
    return this;
  }

  public ResponseBuilder setHeader(final CharSequence name, final Object value) {
    createHeadersIfNeeded();
    headers.set(name, value);
    return this;
  }

  public ResponseBuilder setHeader(final CharSequence name, final Iterable<?> values) {
    createHeadersIfNeeded();
    headers.set(name, values);
    return this;
  }

  public ResponseBuilder addHeader(final CharSequence name, final Object value) {
    createHeadersIfNeeded();
    headers.add(name, value);
    return this;
  }

  public ResponseBuilder addHeader(final CharSequence name, final Iterable<?> values) {
    createHeadersIfNeeded();
    headers.add(name, values);
    return this;
  }

  public ResponseBuilder setContentType(final String contentType) {
    addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
    return this;
  }

  public ResponseBuilder addCookie(final String name, final String value) {
    return bakeCookie(name, value).bake();
  }

  public ResponseBuilder addCookie(final String rawCookie) {
    return addHeader(HttpHeaderNames.SET_COOKIE, rawCookie);
  }

  public CookieBaker bakeCookie(final String name, final String value) {
    return new CookieBaker(name, value);
  }

  private void createHeadersIfNeeded() {
    if (headers == null) {
      withHeaders(new DefaultHttpHeaders(true));
    }
  }

  public Response build() {
    return new NettyResponse(status, rawContent, message, headers);
  }

  public class CookieBaker {

    private final DefaultCookie cookie;

    public CookieBaker(final String name, final String value) {
      cookie = new DefaultCookie(name, value);
    }

    public ResponseBuilder bake() {
      addCookie(ServerCookieEncoder.STRICT.encode(cookie));
      return ResponseBuilder.this;
    }

    public CookieBaker setMaxAge(final long maxAge) {
      cookie.setMaxAge(maxAge);
      return this;
    }

    public CookieBaker setHttpOnly(final boolean httpOnly) {
      cookie.setHttpOnly(httpOnly);
      return this;
    }

    public CookieBaker setDomain(final String domain) {
      cookie.setDomain(domain);
      return this;
    }

    public CookieBaker setPath(final String path) {
      cookie.setPath(path);
      return this;
    }

    public CookieBaker setComment(final String comment) {
      cookie.setComment(comment);
      return this;
    }

    public CookieBaker setVersion(final int version) {
      cookie.setVersion(version);
      return this;
    }

    public CookieBaker setSecure(final boolean secure) {
      cookie.setSecure(secure);
      return this;
    }

    public CookieBaker setCommentUrl(final String commentUrl) {
      cookie.setCommentUrl(commentUrl);
      return this;
    }

    public CookieBaker setDiscard(final boolean discard) {
      cookie.setDiscard(discard);
      return this;
    }

    public CookieBaker setPorts(final int... ports) {
      cookie.setPorts(ports);
      return this;
    }
  }
}
