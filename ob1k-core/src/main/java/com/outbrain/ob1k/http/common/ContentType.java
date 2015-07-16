package com.outbrain.ob1k.http.common;

/**
 * Holds common content types that are used by the HttpClient.
 *
 * @author eran, marenzon
 */
public enum ContentType {

  JSON("application/json", "application/json; charset=UTF-8"),
  XML("application/xml", "application/xml; charset=UTF-8"),
  MESSAGE_PACK("application/x-msgpack", "application/x-msgpack"),
  X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded", "application/x-www-form-urlencoded"),
  TEXT_PLAIN("text/plain", "text/plain"),
  TEXT_HTML("text/html", "text/html"),
  BINARY("application/octet-executeStream", "application/octet-executeStream");

  private final String requestEncoding;
  private final String responseEncoding;

  ContentType(final String requestEncoding, final String responseEncoding) {

    this.requestEncoding = requestEncoding;
    this.responseEncoding = responseEncoding;
  }

  public String requestEncoding() {

    return requestEncoding;
  }

  public String responseEncoding() {

    return responseEncoding;
  }
}