package com.outbrain.ob1k.server.netty;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.outbrain.ob1k.common.marshalling.ChunkHeader;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.server.ResponseHandler;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.StaticPathResolver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.CharsetUtil;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 10:30 AM
 */
public class HttpRequestDispatcherHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcherHandler.class);

  private final StaticPathResolver staticResolver;

  private final ServiceDispatcher dispatcher;
  private final String contextPath;
  private final RequestMarshallerRegistry marshallerRegistry;
  private final boolean acceptKeepAlive;

  private final ChannelGroup activeChannels;

  private final Counter internalErrors;
  private final Counter requestTimeoutErrors;
  private final Counter notFoundErrors;
  private final Counter unexpectedErrors;
  private final Counter ioErrors;
  private final long requestTimeoutMs;

  private io.netty.handler.codec.http.HttpRequest request;
  private Subscription subscription;

  HttpRequestDispatcherHandler(final String contextPath,
                               final ServiceDispatcher dispatcher,
                               final StaticPathResolver staticResolver,
                               final RequestMarshallerRegistry marshallerRegistry,
                               final ChannelGroup activeChannels,
                               final boolean acceptKeepAlive,
                               final MetricFactory metricFactory,
                               final long requestTimeoutMs) {
    this.dispatcher = dispatcher;
    this.staticResolver = staticResolver;
    this.contextPath = contextPath;
    this.marshallerRegistry = marshallerRegistry;
    this.activeChannels = activeChannels;
    this.acceptKeepAlive = acceptKeepAlive;
    this.requestTimeoutMs = requestTimeoutMs;

    if (metricFactory != null) {
      this.internalErrors = metricFactory.createCounter("Ob1kDispatcher", "internalErrors");
      this.requestTimeoutErrors = metricFactory.createCounter("Ob1kDispatcher", "requestTimeoutErrors");
      this.notFoundErrors = metricFactory.createCounter("Ob1kDispatcher", "notFoundErrors");
      this.unexpectedErrors = metricFactory.createCounter("Ob1kDispatcher", "unexpectedErrors");
      this.ioErrors = metricFactory.createCounter("Ob1kDispatcher", "ioErrors");
      metricFactory.registerGauge("Ob1kDispatcher", "currentConnections", activeChannels::size);
    } else {
      internalErrors = null;
      requestTimeoutErrors = null;
      notFoundErrors = null;
      unexpectedErrors = null;
      ioErrors = null;
    }
  }

  @Override
  public void channelReadComplete(final io.netty.channel.ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
    // if connection is idle for more than X millis, close it
    if (evt instanceof IdleStateEvent) {
      final IdleStateEvent e = (IdleStateEvent) evt;
      if (IdleState.ALL_IDLE == e.state()) {
        ctx.close();
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws IOException {
    if (msg instanceof HttpRequest) {
      request = (HttpRequest) msg;

      final String uri = request.getUri();
      final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
      final String path = queryStringDecoder.path();
      if (!path.startsWith(contextPath)) {
        handleNotFound(uri, ctx);
        return;
      }

      if (staticResolver.isStaticPath(uri)) {
        ctx.fireChannelRead(msg);
        return;
      }

      if (is100ContinueExpected(request)) {
        ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
      }
    }

    if (msg instanceof LastHttpContent) {
      final HttpContent req = (HttpContent) msg;

      try {
        dispatcher.callServiceRequest(new NettyRequest(request, req, ctx.channel(), contextPath), new ResponseHandler() {
          @Override
          public void handleAsyncResponse(final ComposableFuture<Object> response) {
            HttpRequestDispatcherHandler.this.handleAsyncResponse(ctx, response);
          }

          @Override
          public void handleStreamResponse(final Observable<Object> response, final boolean rawStream) {
            HttpRequestDispatcherHandler.this.handleStreamResponse(ctx, response, rawStream);
          }
        });
      } catch (final IOException error) {
        handleInternalError(error, ctx);
      } catch (final Exception error) {
        handleUnexpectedRequest(error, ctx);
      }
    }
  }

  private void handleAsyncResponse(final ChannelHandlerContext ctx, final ComposableFuture<Object> response) {
    final ComposableFuture<Object> finalResponse;
    if (requestTimeoutMs > 0) {
      final ComposableFuture<Object> timeout = scheduleRequestTimeout(ctx);
      finalResponse = ComposableFutures.any(response, timeout);
    } else {
      finalResponse = response;
    }

    finalResponse.consume(result -> {
      try {
        if (result.isSuccess()) {
          handleOK(result.getValue(), ctx);
        } else {
          final Throwable error = result.getError();
          if (error instanceof RequestTimeoutException) {
            if (requestTimeoutErrors != null) {
              requestTimeoutErrors.inc();
            }
          }
          handleInternalError(error, ctx);
        }
      } catch (final IOException error) {
        handleInternalError(error, ctx);
      }
    });


  }

  private void handleStreamResponse(final ChannelHandlerContext ctx, final Observable<Object> response, final boolean rawStream) {
    // first send the packet containing the headers.
    sendStreamHeaders(ctx, rawStream);
    subscription = response.subscribe(new Subscriber<Object>() {
      @Override
      public void onCompleted() {
        final LastHttpContent chunk = new DefaultLastHttpContent();
        final ChannelFuture channelFuture = ctx.writeAndFlush(chunk);
        channelFuture.addListener(ChannelFutureListener.CLOSE);
      }

      @Override
      public void onError(final Throwable e) {
        final String content = rawStream ? e.toString() : ChunkHeader.ERROR_HEADER + e.toString();
        final ByteBuf buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
        final LastHttpContent chunk = new DefaultLastHttpContent(buf);
        ctx.writeAndFlush(chunk).addListener(ChannelFutureListener.CLOSE);
      }

      @Override
      public void onNext(final Object object) {
        if (!ctx.channel().isActive()) {
          unsubscribe();
          return;
        }

        try {
          sendStreamChunk(object, ctx, rawStream);
        } catch (final IOException e) {
          unsubscribe();
          onError(e);
        }
      }
    });
  }

  private ChannelFuture sendStreamChunk(final Object message, final ChannelHandlerContext ctx, final boolean rawStream) throws IOException {
    final RequestMarshaller marshaller = getMarshaller(request);
    final HttpContent chunk = marshaller.marshallResponsePart(message, OK, rawStream);

    return ctx.writeAndFlush(chunk);
  }

  private ChannelFuture sendStreamHeaders(final ChannelHandlerContext ctx, final boolean rawStream) {
    final RequestMarshaller marshaller = getMarshaller(request);
    final HttpResponse res = marshaller.marshallResponseHeaders(rawStream);

    return ctx.writeAndFlush(res);
  }

  private ComposableFuture<Object> scheduleRequestTimeout(final ChannelHandlerContext ctx) {
    return ComposableFutures.build(consumer -> ctx.channel()
      .eventLoop().schedule(
        () -> consumer.consume(Try.fromError(new RequestTimeoutException("calculating response took too long."))),
        requestTimeoutMs,
        TimeUnit.MILLISECONDS
      )
    );
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    if (subscription != null) {
      subscription.unsubscribe();
    }

    if (unexpectedErrors != null) {
      unexpectedErrors.inc();
    }

    // suppressing IO exceptions - as mostly they're only creating noise
    if (cause instanceof IOException) {
      if (ioErrors != null) {
        ioErrors.inc();
      }

      logger.debug("caught IO exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    } else {
      logger.warn("caught exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    }

    ctx.close();
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    if (subscription != null) {
      subscription.unsubscribe();
    }

    super.channelInactive(ctx);
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
    if (subscription != null) {
      subscription.unsubscribe();
    }

    super.channelUnregistered(ctx);
  }

  private void handleInternalError(final Throwable error, final ChannelHandlerContext ctx) {
    if (internalErrors != null) {
      internalErrors.inc();
    }

    logger.warn("Internal error while processing URI: " + request.getUri() + " from remote address " + ctx.channel().remoteAddress(), error);
    try {
      handleResponse(error.toString(), INTERNAL_SERVER_ERROR, ctx);
    } catch (final IOException e) {
      logger.warn("cant create a proper error message", e);

      final ByteBuf buf = Unpooled.copiedBuffer(error.toString(), CharsetUtil.UTF_8);
      final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, buf);
      response.headers().set(CONTENT_TYPE, ContentType.JSON.responseEncoding());

      handleResponse(response, ctx);
    }
  }

  private void handleOK(final Object res, final ChannelHandlerContext ctx) throws IOException {
    if (res instanceof NettyResponse) {
      handleResponse((NettyResponse) res, ctx);
    } else {
      handleResponse(res, OK, ctx);
    }
  }

  private void handleResponse(final NettyResponse nettyResponse, final ChannelHandlerContext ctx) throws IOException {
    final RequestMarshaller marshaller = getMarshaller(request);
    final FullHttpResponse response = nettyResponse.toFullHttpResponse(marshaller);
    handleResponse(response, ctx);
  }

  private void handleResponse(final FullHttpResponse response, final ChannelHandlerContext ctx) {
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

    final boolean keepAlive = isKeepAlive(request);
    if (acceptKeepAlive && keepAlive) {
      // Add keep alive header as per:
      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      response.headers().set(CONNECTION, KEEP_ALIVE);
      ctx.writeAndFlush(response);
    } else {
      response.headers().set(CONNECTION, CLOSE);
      ctx.writeAndFlush(response).
        addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void handleResponse(final Object message,
                              final HttpResponseStatus status,
                              final ChannelHandlerContext ctx) throws IOException {
    final RequestMarshaller marshaller = getMarshaller(request);
    final FullHttpResponse response = marshaller.marshallResponse(message, status);
    handleResponse(response, ctx);
  }

  private RequestMarshaller getMarshaller(final HttpRequest request) {
    return marshallerRegistry.getMarshaller(request.headers().get(CONTENT_TYPE));
  }

  private void handleUnexpectedRequest(final Exception error, final ChannelHandlerContext ctx) throws IOException {
    if (unexpectedErrors != null) {
      unexpectedErrors.inc();
    }

    if (error instanceof IllegalArgumentException) {
      // stack-trace not interesting, as the exception probably because of invocation failure
      logger.info("The requested URI isn't supported: {}", request.getUri());
      logger.debug("Invocation error: ", error);
    } else {
      logger.info("The requested URI isn't supported: {}", request.getUri(), error);
    }
    handleResponse(error.toString(), HttpResponseStatus.NOT_IMPLEMENTED, ctx);
  }

  private void handleNotFound(final String uri, final ChannelHandlerContext ctx) throws IOException {
    if (notFoundErrors != null) {
      notFoundErrors.inc();
    }

    logger.info("Requested URI was not found: {}", uri);
    handleResponse(uri + " is not a valid request path", HttpResponseStatus.NOT_FOUND, ctx);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().remoteAddress() != null) {
      activeChannels.add(ctx.channel());
    }
    super.channelActive(ctx);
  }

}
