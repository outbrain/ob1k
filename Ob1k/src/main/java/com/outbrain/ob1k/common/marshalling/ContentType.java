package com.outbrain.ob1k.common.marshalling;

/**
* Time: 1/21/14 11:53 AM
*
* @author Eran Harel
*/
public enum ContentType {
  JSON("application/json", "application/json; charset=UTF-8"),
  MESSAGE_PACK("application/x-msgpack", "application/x-msgpack"),
  X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded", "application/x-www-form-urlencoded"),
  TEXT_HTML("text/html", "text/html"),
  TEXT_PLAIN("text/plain", "text/plain");

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
