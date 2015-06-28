package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.ning.http.client.Response;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * @author marenzon
 */
public class NingResponse<T> implements TypedResponse<T> {

  private final UnmarshallingStrategy unmarshallingStrategy;
  private final Type type;
  private final Response ningResponse;
  private List<Cookie> cookies;
  private T typedBody;

  public NingResponse(final Response ningResponse, final Type type, final UnmarshallingStrategy unmarshallingStrategy) throws IOException {

    checkNotNull(ningResponse, "ningResponse may not be null");

    this.ningResponse = ningResponse;
    this.unmarshallingStrategy = unmarshallingStrategy;
    this.type = type;
  }

  @Override
  public int getStatusCode() {

    return ningResponse.getStatusCode();
  }

  @Override
  public String getStatusText() {

    return ningResponse.getStatusText();
  }

  @Override
  public URI getUri() throws URISyntaxException {

    return ningResponse.getUri().toJavaNetURI();
  }

  @Override
  public String getContentType() {

    return ningResponse.getContentType();
  }

  @Override
  public T getTypedBody() throws IOException {

    if (typedBody == null) {

      checkNotNull(unmarshallingStrategy, "unmarshallingStrategy may not be null");
      checkNotNull(type, "class type may not be null");

      typedBody = unmarshallingStrategy.unmarshall(type, this);
    }

    return typedBody;
  }

  @Override
  public String getResponseBody() throws IOException {

    return ningResponse.getResponseBody();
  }

  @Override
  public byte[] getResponseBodyAsBytes() throws IOException {

    return ningResponse.getResponseBodyAsBytes();
  }

  @Override
  public InputStream getResponseBodyAsStream() throws IOException {

    return ningResponse.getResponseBodyAsStream();
  }

  @Override
  public List<Cookie> getCookies() {

    if (cookies == null) {

      cookies = transformNingResponseCookies(ningResponse.getCookies());
    }

    return cookies;
  }

  @Override
  public String getHeader(final String name) {

    return ningResponse.getHeader(name);
  }

  @Override
  public List<String> getHeaders(final String name) {

    return ningResponse.getHeaders(name);
  }

  @Override
  public Map<String, List<String>> getHeaders() {

    return ningResponse.getHeaders();
  }

  @Override
  public boolean isRedirected() {

    return ningResponse.isRedirected();
  }

  private List<Cookie> transformNingResponseCookies(final List<com.ning.http.client.cookie.Cookie> cookies) {

    final Function<com.ning.http.client.cookie.Cookie, Cookie> transformer = new Function<com.ning.http.client.cookie.Cookie, Cookie>() {
      @Override
      public Cookie apply(final com.ning.http.client.cookie.Cookie ningCookie) {
        return new Cookie(ningCookie.getName(), ningCookie.getValue(), ningCookie.getDomain(), ningCookie.getPath(), ningCookie.getMaxAge(),
          ningCookie.isSecure(), ningCookie.isHttpOnly());
      }
    };

    return Lists.transform(cookies, transformer);
  }
}