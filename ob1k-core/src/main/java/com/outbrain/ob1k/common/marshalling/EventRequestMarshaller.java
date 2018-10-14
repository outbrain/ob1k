package com.outbrain.ob1k.common.marshalling;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

public class EventRequestMarshaller extends JsonRequestMarshaller {
  private final ObjectMapper om = new ObjectMapper();

  @Override
  public HttpContent marshallResponsePart(Object message, HttpResponseStatus status, boolean rawStream) throws IOException {
    final String content = "data: " + om.writeValueAsString(message) + "\n\n";
    final ByteBuf buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
    final DefaultHttpContent defaultHttpContent = new DefaultHttpContent(buf);
    return defaultHttpContent;
  }

  @Override
  public HttpResponse marshallResponseHeaders(final boolean rawStream) {
    final HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    res.headers().add(CONNECTION, KEEP_ALIVE);
    res.headers().add(CONTENT_TYPE.toLowerCase(), "text/event-stream");
    res.headers().add(TRANSFER_ENCODING, "UTF-8");
    res.headers().add(CONTENT_ENCODING, "UTF-8");
    res.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    return res;
  }

}
