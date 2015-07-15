package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.ning.http.client.Response;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author marenzon
 */
public class NingResponse<T> implements TypedResponse<T> {

  private final MarshallingStrategy marshallingStrategy;
  private final Type type;
  private final Response ningResponse;
  private volatile T typedBody;

  public NingResponse(final Response ningResponse, final Type type, final MarshallingStrategy marshallingStrategy) throws IOException {

    checkNotNull(ningResponse, "ningResponse may not be null");

    this.ningResponse = ningResponse;
    this.marshallingStrategy = marshallingStrategy;
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
  public String getUrl() {

    return ningResponse.getUri().toUrl();
  }

  @Override
  public String getContentType() {

    return ningResponse.getContentType();
  }

  @Override
  public T getTypedBody() throws IOException {

    if (typedBody == null) {

      checkNotNull(marshallingStrategy, "unmarshallingStrategy may not be null");
      checkNotNull(type, "class type may not be null");

      typedBody = marshallingStrategy.unmarshall(type, this);
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
  public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {

    return ningResponse.getResponseBodyAsByteBuffer();
  }

  @Override
  public List<Cookie> getCookies() {

    return transformNingResponseCookies(ningResponse.getCookies());
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

  @Override
  public boolean hasResponseBody() {

    return ningResponse.hasResponseBody();
  }

  @Override
  public boolean hasResponseStatus() {

    return ningResponse.hasResponseStatus();
  }

  @Override
  public boolean hasResponseHeaders() {

    return ningResponse.hasResponseHeaders();
  }

  private List<Cookie> transformNingResponseCookies(final List<com.ning.http.client.cookie.Cookie> cookies) {

    final Function<com.ning.http.client.cookie.Cookie, Cookie> transformer = new Function<com.ning.http.client.cookie.Cookie, Cookie>() {
      @Override
      public Cookie apply(final com.ning.http.client.cookie.Cookie ningCookie) {
        return new Cookie(ningCookie.getName(), ningCookie.getValue(), ningCookie.getDomain(), ningCookie.getPath(),
                ningCookie.getMaxAge(), ningCookie.getExpires(), ningCookie.isSecure(), ningCookie.isHttpOnly());
      }
    };

    return Lists.transform(cookies, transformer);
  }
}