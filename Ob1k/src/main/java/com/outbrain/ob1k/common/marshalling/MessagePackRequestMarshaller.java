package com.outbrain.ob1k.common.marshalling;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.outbrain.ob1k.Request;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.Template;
import org.msgpack.template.builder.TemplateBuildException;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Values.CHUNKED;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * User: aronen
 * Date: 8/18/13
 * Time: 3:33 PM
 */
public class MessagePackRequestMarshaller implements RequestMarshaller {
  private static final Logger logger = LoggerFactory.getLogger(MessagePackRequestMarshaller.class);
  private static final byte[] NEW_LINE = "\n".getBytes(CharsetUtil.UTF_8);
  private static final byte[] HTML_NEW_LINE = "<br/>\n".getBytes(CharsetUtil.UTF_8);
  private static final byte[] HEADER = ChunkHeader.ELEMENT_HEADER.getBytes(CharsetUtil.UTF_8);

  private MessagePack msgPack;

  public MessagePackRequestMarshaller() {
    msgPack = new MessagePack();
  }

  @Override
  public void registerTypes(final Type... types) throws MessageTypeException {
    registerTypes(new HashSet<Class>(), types);
  }

  public void registerTypes(final Set<Class> processed, final Type... types) throws MessageTypeException {
    for(final Type type: types) {
      if (type == null)
        continue;

      if (type instanceof Class) {
        final Class cls = (Class) type;
        if (processed.contains(cls))
          continue;

        if (!Modifier.isAbstract(cls.getModifiers()) || cls.isEnum()) {
          boolean found;
          try {
            found = (msgPack.lookup(cls) != null);
          } catch (final MessageTypeException e) {
            found = false;
          }
          if (!found) {
            registerBean(processed, cls);
          }
        } else if(cls.isArray()) {
          registerTypes(processed, cls.getComponentType());
        }

      } else if (type instanceof ParameterizedType) {
        final ParameterizedType paramType = (ParameterizedType) type;
        final Type[] actualTypes = paramType.getActualTypeArguments();
        registerTypes(processed, actualTypes);
        registerTypes(processed, paramType.getOwnerType());
        registerTypes(processed, paramType.getRawType());

      } else if (type instanceof GenericArrayType) {
        final GenericArrayType genType = (GenericArrayType) type;
        registerTypes(processed, genType.getGenericComponentType());

      } else if (type instanceof TypeVariable) {
        final TypeVariable varType = (TypeVariable) type;
        registerTypes(processed, varType.getBounds());

      }
    }
  }

  private void registerBean(final Set<Class> processed, final Class cls) {
    if (cls == Request.class || cls == HttpRequest.class || cls == HttpResponse.class)
      return;

    processed.add(cls);
    final Field[] declaredFields = cls.getDeclaredFields();
    for (final Field field: declaredFields) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      final Type fieldType = field.getGenericType();
      registerTypes(processed, fieldType);
    }

    try {
      msgPack.register(cls);
    } catch (MessageTypeException | TemplateBuildException e) {
      logger.warn("class " + cls.getName() + " is not MsgPack compatible. class must have empty constructor, all fields must be concrete and have getters and setters");
      throw e;
    }
  }

  @Override
  public Object[] unmarshallRequestParams(final Request request, final Method method, final String[] paramNames) throws IOException {
    final Type[] types = method.getGenericParameterTypes();
    final Object[] res = new Object[types.length];
    final InputStream inputStream = request.getRequestInputStream();
    final Value rawValues = msgPack.read(inputStream);
    final Value[] values = rawValues.asArrayValue().getElementArray();

    int index = 0;
    for (final Type type : types) {
      final Template template = msgPack.lookup(type);
      res[index] = template.read(new Converter(msgPack, values[index]), null);
      index++;
    }

    return res;
  }

  @Override
  public FullHttpResponse marshallResponse(final Object res, final HttpResponseStatus status) throws IOException {
    final byte[] content = msgPack.write(res);
    final ByteBuf buf = Unpooled.copiedBuffer(content);
    final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buf);

    response.headers().set(CONTENT_TYPE, ContentType.MESSAGE_PACK.responseEncoding());
    return response;
  }

  @Override
  public HttpResponse marshallResponseHeaders(final boolean rawStream) {
    final HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    res.headers().add(TRANSFER_ENCODING, CHUNKED);
    res.headers().add(CONNECTION, KEEP_ALIVE);
    res.headers().add(CONTENT_TYPE, rawStream ? ContentType.TEXT_HTML.responseEncoding() : ContentType.MESSAGE_PACK.responseEncoding());

    return res;
  }


  @Override
  public HttpContent marshallResponsePart(final Object res, final HttpResponseStatus status, final boolean rawStream) throws IOException {
    final byte[] content = msgPack.write(res);
    final ByteBuf buf = rawStream ?
        Unpooled.copiedBuffer(content, HTML_NEW_LINE) :
        Unpooled.copiedBuffer(HEADER, content, NEW_LINE);

    return new DefaultHttpContent(buf);
  }

  @Override
  public void marshallRequestParams(final AsyncHttpClient.BoundRequestBuilder requestBuilder, final Object[] requestParams) throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    final Packer packer = msgPack.createPacker(outputStream);
    packer.writeArrayBegin(requestParams.length);
    for (final Object param : requestParams) {
      packer.write(param);
    }
    packer.writeArrayEnd();

    final byte[] body = outputStream.toByteArray();
    requestBuilder.setBody(body);
    requestBuilder.setContentLength(body.length);
    requestBuilder.setHeader("Content-Type", ContentType.MESSAGE_PACK.requestEncoding());
  }

  @Override
  public Object unmarshallResponse(final Response httpResponse, final Type resType, final boolean failOnError) throws IOException {
    final int statusCode = httpResponse.getStatusCode();
    if (!failOnError || (statusCode >= 200 && statusCode < 300)) {
      final byte[] body = httpResponse.getResponseBodyAsBytes();
      final Template template = msgPack.lookup(resType);
      final Value value = msgPack.read(body);
      return template.read(new Converter(msgPack, value), null);
    } else {
      throw new IOException(httpResponse.getResponseBody());
    }
  }
}
