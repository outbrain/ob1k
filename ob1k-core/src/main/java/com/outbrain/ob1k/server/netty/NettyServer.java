package com.outbrain.ob1k.server.netty;

import static com.outbrain.ob1k.server.cors.CorsConverter.convertHandler;

import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.StaticPathResolver;
import com.outbrain.ob1k.server.cors.CorsConfig;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 10:27 AM
 */
public class NettyServer implements Server {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

  private final int port;
  private final String contextPath;
  private final RequestMarshallerRegistry marshallerRegistry;
  private final ChannelGroup activeChannels;
  private final long requestTimeoutMs;
  private final long idleTimeoutMs;
  private volatile Channel channel;
  private final StaticPathResolver staticResolver;
  private final ServiceDispatcher dispatcher;
  private final EventLoopGroup nioGroup;
  private final String applicationName;
  private final boolean acceptKeepAlive;
  private final boolean supportZip;
  private final MetricFactory metricFactory;
  private final int maxContentLength;
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  private final CorsConfig corsConfig;

  public NettyServer(final int port, final ServiceRegistry registry, final RequestMarshallerRegistry marshallerRegistry,
                     final StaticPathResolver staticResolver,
                     final ChannelGroup activeChannels, final String contextPath, final String applicationName,
                     final boolean acceptKeepAlive, final long idleTimeoutMs, final boolean supportZip, final MetricFactory metricFactory,
                     final int maxContentLength, final long requestTimeoutMs, final CorsConfig corsConfig) {
    System.setProperty("com.outbrain.web.context.path", contextPath);
    this.port = port;
    this.staticResolver = staticResolver;
    this.activeChannels = activeChannels;
    this.contextPath = contextPath;
    this.applicationName = applicationName;
    this.marshallerRegistry = marshallerRegistry;
    this.dispatcher = new ServiceDispatcher(registry, marshallerRegistry);
    this.nioGroup = new NioEventLoopGroup();
    this.acceptKeepAlive = acceptKeepAlive;
    this.supportZip = supportZip;
    this.metricFactory = metricFactory;
    this.maxContentLength = maxContentLength;
    this.requestTimeoutMs = requestTimeoutMs;
    this.idleTimeoutMs = idleTimeoutMs;
    this.corsConfig = corsConfig;
    registry.logRegisteredEndpoints();
  }

  @Override
  public InetSocketAddress start() {
    logger.info("################## Starting OB1K server for module '{}' ##################", applicationName);
    try {
      final ServerBootstrap b = new ServerBootstrap();
      b.option(ChannelOption.SO_BACKLOG, 1024);
      b.option(ChannelOption.SO_RCVBUF, 64 * 1024);
      b.option(ChannelOption.SO_SNDBUF, 64 * 1024);
      // it means that the max static file can be 1024*ResourceRegion.BUFFER_SIZE = 64Mb
      b.childOption(ChannelOption.WRITE_SPIN_COUNT, 1024);
      b.childOption(ChannelOption.TCP_NODELAY, true);
      b.group(nioGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new RPCServerInitializer(maxContentLength));

      channel = b.bind(port).sync().channel();
      addShutdownhook();
      // TEMP disable till I get an answer to https://groups.google.com/d/topic/netty/uY4n1Wjmpvs/discussion
//      NettyQueuesGaugeBuilder.registerQueueGauges(metricFactory, nioGroup, applicationName);

      final InetSocketAddress address = (InetSocketAddress) channel.localAddress();
      onStarted();
      logger.info("server is up and bounded on address: {}{}", address, getOpeningText());
      return address;
    } catch (final Exception e) {
      logger.error("failed to start server", e);
      return null;
    }
  }

  private void addShutdownhook() {
    Runtime.getRuntime().addShutdownHook(new Thread(NettyServer.this::stop));
  }

  private static String getOpeningText() {
    final InputStream inputStream = NettyServer.class.getResourceAsStream("/litany-against-fear.txt");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CharsetUtil.UTF_8))) {
      String line;
      final StringBuilder buffer = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        buffer.append(line).append("\n");
      }

      return buffer.toString();
    } catch (final IOException e) {
      logger.warn("can't read opening text resource");
      return "";
    }
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public int getPort() {
    return ((InetSocketAddress) channel.localAddress()).getPort();
  }

  @Override
  public String getApplicationName() {
    return applicationName;
  }

  @Override
  public void stop() {
    logger.info("################## Stopping OB1K server for module '{}' ##################", applicationName);
    if (!channel.isOpen()) {
      return;
    }
    channel.closeFuture().addListener(future -> {
      logger.info("################## Closing OB1K server threads for module '{}' ##################", applicationName);
      nioGroup.shutdownGracefully();
    });

    logger.info("################## Closing OB1K server socket for module '{}' ##################", applicationName);
    channel.close();
  }

  @Override
  public void addListener(final Listener listener) {
    listeners.add(listener);
  }

  public void addListeners(final Collection<Listener> moreListeners) {
    listeners.addAll(moreListeners);
  }

  @Override
  public void removeListener(final Listener listener) {
    listeners.remove(listener);
  }

  private void onStarted() {
    logger.info("**************** Module '{}' Started ****************", applicationName);
    for (final Listener listener : listeners) {
      listener.serverStarted(this);
    }
  }

  private class RPCServerInitializer extends ChannelInitializer<SocketChannel> {

    private final int maxContentLength;

    RPCServerInitializer(final int maxContentLength) {
      this.maxContentLength = maxContentLength;
    }

    private final HttpStaticFileServerHandler staticFileServerHandler = new HttpStaticFileServerHandler(staticResolver);

    @Override
    public void initChannel(final SocketChannel ch) throws Exception {
      final ChannelPipeline p = ch.pipeline();

      // Uncomment the following line if you want HTTPS
      //SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
      //engine.setUseClientMode(false);
      //p.addLast("ssl", new SslHandler(engine));

      p.addLast("decoder", new HttpRequestDecoder(16384, 8192, 16384));
      p.addLast("encoder", new HttpResponseEncoder());
      p.addLast("aggregator", new HttpObjectAggregator(maxContentLength));

      p.addLast("chunkedWriter", new ChunkedWriteHandler());
      p.addLast("static", staticFileServerHandler);

      // the compressor is behind the static handler to avoid compression of static files
      // Netty doesn't handle it very well :(
      if (supportZip) {
        p.addLast("compressor", new HttpContentCompressor());
      }

      p.addLast("idleState", new IdleStateHandler(0, 0, idleTimeoutMs, TimeUnit.MILLISECONDS));

      if (corsConfig.isCorsSupportEnabled()) {
        p.addLast("cors", convertHandler(corsConfig));
      }

      p.addLast("handler", new HttpRequestDispatcherHandler(contextPath, dispatcher, staticResolver,
          marshallerRegistry, activeChannels, acceptKeepAlive, metricFactory, requestTimeoutMs));
    }
  }

}
