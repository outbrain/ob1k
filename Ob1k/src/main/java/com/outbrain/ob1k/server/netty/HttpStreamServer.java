package com.outbrain.ob1k.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.CHUNKED;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

public class HttpStreamServer {
  public static class StreamCounter {
    public StreamCounter(final String name) {
      this.name = name;
      this.counter = 0;
    }

    public final String name;
    public int counter;
  }

  public static void main(final String[] args) {
    final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    final ConcurrentMap<ChannelHandlerContext, StreamCounter> channels = new ConcurrentHashMap<>();
    final Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        for (final ChannelHandlerContext ctx : channels.keySet()) {
          final StreamCounter entry = channels.get(ctx);

          if (entry.counter == 100) {
            channels.remove(ctx);

            final ByteBuf buf = Unpooled.copiedBuffer("the end.", CharsetUtil.UTF_8);
            final LastHttpContent chunk = new DefaultLastHttpContent(buf);
            chunk.trailingHeaders().add("Success", "false");
            final ChannelFuture channelFuture = ctx.writeAndFlush(chunk);
            channelFuture.addListener(ChannelFutureListener.CLOSE);

          } else {
            final String content = "this is message #" + entry.counter + " for: " + entry.name + "\n";
            final ByteBuf buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);

            entry.counter++;

            final HttpContent chunk = new DefaultHttpContent(buf);
            ctx.writeAndFlush(chunk);
          }
        }
      }
    }, 0, 100);


    try {
      final ServerBootstrap b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
              final ChannelPipeline p = ch.pipeline();

              p.addLast("codec", new HttpServerCodec());
              p.addLast("handler", new StreamHandler(channels));
            }
          });

      final Channel ch = b.bind(8080).sync().channel();
      System.out.println("channel is bounded on 8080");
      ch.closeFuture().sync();
    } catch (final InterruptedException ex) {
      System.out.println("walla...");
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static class StreamHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private final ConcurrentMap<ChannelHandlerContext, StreamCounter> channels;

    public StreamHandler(final ConcurrentMap<ChannelHandlerContext, StreamCounter> channels) {
      super(HttpRequest.class, true);
      this.channels = channels;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest msg) throws Exception {
      final QueryStringDecoder decoder = new QueryStringDecoder(msg.getUri());
      final List<String> names = decoder.parameters().get("name");
      if (names == null || names.isEmpty()) {
        return;
      }

      final String name = names.get(0);
      final HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
//      final ByteBuf initialBuf = Unpooled.copiedBuffer("this ia an initial text to be displayed at the beginning...", CharsetUtil.UTF_8);
//      final HttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, initialBuf);
      res.headers().add(TRANSFER_ENCODING, CHUNKED);
      res.headers().add(CONNECTION, KEEP_ALIVE);
//      res.headers().add(CONTENT_TYPE, "application/json");
      res.headers().add(CONTENT_TYPE, "text/plain");
      res.headers().add(TRAILER, "Success");

      ctx.writeAndFlush(res);
      channels.put(ctx, new StreamCounter(name));
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
      System.out.println("channelUnregistered");
      channels.remove(ctx);
      super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
      System.out.println("get exception");
      cause.printStackTrace();
      super.exceptionCaught(ctx, cause);
    }
  }
}
