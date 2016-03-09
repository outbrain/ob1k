package com.outbrain.ob1k.common.marshalling;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.http.marshalling.MessagePackMarshallingStrategy;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.Template;
import org.msgpack.template.builder.BuildContext;
import org.msgpack.template.builder.TemplateBuildException;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
  private static final java.util.logging.Logger msgPackLogger = java.util.logging.Logger.getLogger(BuildContext.class.getName());
  private static final byte[] NEW_LINE = "\n".getBytes(CharsetUtil.UTF_8);
  private static final byte[] HTML_NEW_LINE = "<br/>\n".getBytes(CharsetUtil.UTF_8);
  private static final byte[] HEADER = ChunkHeader.ELEMENT_HEADER.getBytes(CharsetUtil.UTF_8);

  private final MarshallingStrategy msgPackMarshallingStrategy;
  private final MessagePack msgPack;

  public MessagePackRequestMarshaller() {
    msgPack = new MessagePack();
    msgPackMarshallingStrategy = new MessagePackMarshallingStrategy(msgPack);
  }

  @Override
  public void registerTypes(final Type... types) throws MessageTypeException {
    registerTypes(new HashSet<>(), types);
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

  @Override
  public Object[] unmarshallRequestParams(final Request request, final Method method, final String[] paramNames) throws IOException {
    // if the method is not expecting anything, no reason trying unmarshalling
    if (paramNames.length == 0) {
      return new Object[0];
    }

    final Type[] types = method.getGenericParameterTypes();
    final List<Object> results = new ArrayList<>(types.length);
    final InputStream inputStream = request.getRequestInputStream();
    final Map<String, String> pathParams = request.getPathParams();

    int index = 0;
    for (final String paramName: paramNames) {
      if (pathParams.containsKey(paramName)) {
        results.add(ParamMarshaller.unmarshall(pathParams.get(paramName), (Class) types[index]));
        index++;
      } else {
        break;
      }
    }

    if (results.size() < pathParams.size()) {
      throw new IOException("path params should be bounded to be a prefix of the method parameters list.");
    }

    final HttpRequestMethodType requestMethod = request.getMethod();
    if (HttpRequestMethodType.GET == requestMethod || HttpRequestMethodType.DELETE == requestMethod) {
      final Map<String, String> queryParams = request.getQueryParams();
      for (final String paramName : paramNames) {
        if (queryParams.containsKey(paramName)) {
          final Type type = types[index];
          if (!isPrimitiveOrString(type)) {
            throw new IllegalArgumentException("only primitives and strings are allowed in query");
          }
          results.add(ParamMarshaller.unmarshall(queryParams.get(paramName), (Class) type));
          index++;
        } else {
          break;
        }
      }
    }

    if (results.size() == types.length) {
      return results.toArray();
    }

    if (request.getContentLength() == 0) {
      throw new IllegalArgumentException("not enough params passed for the request");
    }

    final Value rawValues = msgPack.read(inputStream);
    final Value[] values = rawValues.asArrayValue().getElementArray();

    final int pathParamsSize = pathParams.size();

    for (; index < types.length; index++) {
      final Template template = msgPack.lookup(types[index]);
      @SuppressWarnings("unchecked")
      final Object unmarshalled = template.read(new Converter(msgPack, values[index - pathParamsSize]), null);
      results.add(unmarshalled);
    }

    return results.toArray();
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
  public byte[] marshallRequestParams(final Object[] requestParams) throws IOException {

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final Packer packer = msgPack.createPacker(outputStream);

    final Object[] params;
    if (requestParams == null) {
      params = new Object[0];
    } else {
      params = requestParams;
    }

    packer.writeArrayBegin(params.length);
    for (final Object param : params) {
      packer.write(param);
    }
    packer.writeArrayEnd();

    return outputStream.toByteArray();
  }

  @Override
  public <T> T unmarshallResponse(final Response response, final Type type) throws IOException {

    return msgPackMarshallingStrategy.unmarshall(type, response);
  }

  @Override
  public <T> T unmarshallStreamResponse(final com.outbrain.ob1k.http.Response response, final Type type) throws IOException {

    final ByteBuffer byteBufferBody = response.getResponseBodyAsByteBuffer();

    if (byteBufferBody.remaining() < HEADER.length) {
      throw new IOException("bad stream response - no chunk header");
    }

    final byte[] header = new byte[HEADER.length];
    byteBufferBody.get(header);

    final int remaining = byteBufferBody.remaining();

    if (Arrays.equals(HEADER, header)) {

      if (remaining == 0) {
        // on empty streamBody the msgpack reader throws EOF
        return null;
      }

      final Value value = msgPack.read(byteBufferBody);
      @SuppressWarnings("unchecked")
      final Template<T> template = (Template<T>) msgPack.lookup(type);
      return template.read(new Converter(msgPack, value), null);

    } else if (Arrays.equals(ChunkHeader.ERROR_HEADER.getBytes(CharsetUtil.UTF_8), header)) {

      final byte[] body = new byte[remaining];
      byteBufferBody.get(body);
      throw new RuntimeException(new String(body, CharsetUtil.UTF_8));
    }

    throw new IOException("invalid chunk header - unsupported " + new String(header, CharsetUtil.UTF_8));
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
      registerWithoutJavaLogs(cls);
    } catch (final MessageTypeException | TemplateBuildException e) {
      logger.warn("class " + cls.getName() + " is not MsgPack compatible. class must have empty constructor, all fields must be concrete and have getters and setters");
      logger.debug("failed registering type", e);
      throw new IllegalArgumentException("'" + cls.getName() + "' is not MsgPack compatible");
    }
  }

  private void registerWithoutJavaLogs(final Class cls) {
    // msgpack logs java logger to log error while trying to register some bean, but it's not really readable output
    // so turning it off
    msgPackLogger.setLevel(Level.OFF);
    msgPack.register(cls);
    // setting the logger back to its default level
    msgPackLogger.setLevel(Level.INFO);
  }

  private boolean isPrimitiveOrString(final Type type) {
    return type instanceof Class && (((Class) type).isPrimitive() || String.class.isAssignableFrom((Class<?>) type));
  }
}