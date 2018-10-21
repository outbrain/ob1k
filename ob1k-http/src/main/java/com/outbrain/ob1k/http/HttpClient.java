package com.outbrain.ob1k.http;

import com.outbrain.ob1k.http.marshalling.JacksonMarshallingStrategy;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import com.outbrain.ob1k.http.ning.NingRequestBuilder;
import com.outbrain.swinfra.metrics.api.MetricFactory;


import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import static org.asynchttpclient.Dsl.asyncHttpClient;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

/**
 * Ob1k's Http Client
 *
 * Provides a simple async client for your http requests,
 * with shared client configuration and specific request configuration.
 *
 * Using AsyncHttpClient as the executor
 *
 * @author marenzon, insipred of aronen's previous HttpClient implemintation.
 */
public class HttpClient implements Closeable {

  public static final int CHUNKED_FILE_CHUNK_SIZE = 65536;
  public static final int RETRIES = 3;
  public static final int CONNECTION_TIMEOUT = 200;
  public static final int REQUEST_TIMEOUT = 500;
  public static final int READ_TIMEOUT = 500;
  public static final int MAX_CONNECTIONS_PER_HOST = 100;
  public static final int MAX_TOTAL_CONNECTIONS = MAX_CONNECTIONS_PER_HOST * 2;

  private final AsyncHttpClient asyncHttpClient;
  private final MarshallingStrategy marshallingStrategy;
  private final long responseMaxSize;

  private HttpClient(final AsyncHttpClient asyncHttpClient, final long responseMaxSize,
                     final MarshallingStrategy marshallingStrategy) {

    this.asyncHttpClient = asyncHttpClient;
    this.responseMaxSize = responseMaxSize;
    this.marshallingStrategy = marshallingStrategy;
  }

  /**
   * Creates new instance of HttpClient with default configuration
   *
   * @return new HttpClient
   */
  public static HttpClient createDefault() {

    return HttpClient.newBuilder().build();
  }

  /**
   * Http get request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder get(final String url) {

    Objects.requireNonNull(url, "url may not be null");
    final BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareGet(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http post request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder post(final String url) {

    Objects.requireNonNull(url, "url may not be null");
    final BoundRequestBuilder ningRequestBuilder = asyncHttpClient.preparePost(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http put request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder put(final String url) {

    Objects.requireNonNull(url, "url may not be null");
    final BoundRequestBuilder ningRequestBuilder = asyncHttpClient.preparePut(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http delete request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder delete(final String url) {

    Objects.requireNonNull(url, "url may not be null");
    final BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareDelete(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http head request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder head(final String url) {

    Objects.requireNonNull(url, "url may not be null");
    final BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareHead(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Closing the http client
   *
   * @throws IOException
   */
  @Override
  public void close() throws IOException {

    asyncHttpClient.close();
  }

  private NingRequestBuilder createNewRequestBuilder(final String url, final BoundRequestBuilder ningRequestBuilder) {

    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, responseMaxSize, marshallingStrategy);
  }

  /**
   * @return new builder object
   */
  public static Builder newBuilder() {

    return new Builder();
  }

  /**
   * Builder for the HttpClient configuration
   * Creates a new HttpClient instance.
   */
  public static class Builder {

    private MarshallingStrategy marshallingStrategy = JacksonMarshallingStrategy.INSTANCE;
    private MetricFactory metricFactory;
    private int connectionTimeout = CONNECTION_TIMEOUT;
    private int requestTimeout = REQUEST_TIMEOUT;
    private int readTimeout = READ_TIMEOUT;
    private int retries = RETRIES;
    private int maxConnectionsPerHost = MAX_CONNECTIONS_PER_HOST;
    private int maxTotalConnections = MAX_TOTAL_CONNECTIONS;
    private int chunkedFileChunkSize = CHUNKED_FILE_CHUNK_SIZE;
    private boolean compressionEnforced;
    private boolean disableUrlEncoding;
    private boolean followRedirect;
    private long responseMaxSize;

    /**
     * Max retries for request
     *
     * @param retries retries count
     * @return builder
     */
    public Builder setRetries(final int retries) {

      this.retries = retries;
      return this;
    }

    /**
     * Max number of connections per host
     *
     * @param maxConnectionsPerHost the max connections number per host
     * @return builder
     */
    public Builder setMaxConnectionsPerHost(final int maxConnectionsPerHost) {

      this.maxConnectionsPerHost = maxConnectionsPerHost;
      return this;
    }

    /**
     * Response max size - in case of large response that may cause OOM
     *
     * @param responseMaxSize response max size
     * @return builder
     */
    public Builder setResponseMaxSize(final long responseMaxSize) {

      this.responseMaxSize = responseMaxSize;
      return this;
    }

    /**
     * Max number of total connections
     *
     * @param maxTotalConnections the max total connections number
     * @return builder
     */
    public Builder setMaxTotalConnections(final int maxTotalConnections) {

      this.maxTotalConnections = maxTotalConnections;
      return this;
    }

    /**
     * Connection timeout
     *
     * @param connectionTimeout connection timeout in ms
     * @return builder
     */
    public Builder setConnectionTimeout(final int connectionTimeout) {

      this.connectionTimeout = connectionTimeout;
      return this;
    }

    /**
     * Request timeout
     * Can be override in request builder phase
     *
     * @param requestTimeout request timeout in ms
     * @return builder
     */
    public Builder setRequestTimeout(final int requestTimeout) {

      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Read timeout (optional, defaults to 60 sec).
     *
     * @param readTimeout read timeout in ms
     * @return builder
     */
    public Builder setReadTimeout(final int readTimeout) {

      this.readTimeout = readTimeout;
      return this;
    }

    /**
     * Enforce compression on the request
     *
     * @param compressionEnforced enforce compression
     * @return builder
     */
    public Builder setCompressionEnforced(final boolean compressionEnforced) {

      this.compressionEnforced = compressionEnforced;
      return this;
    }

    /**
     * Disable url encoding
     *
     * @param disableUrlEncoding disable url encoding
     * @return builder
     */
    public Builder setDisableUrlEncoding(final boolean disableUrlEncoding) {

      this.disableUrlEncoding = disableUrlEncoding;
      return this;
    }

    /**
     * Follow redirects
     *
     * @param followRedirect follow redirects
     * @return builder
     */
    public Builder setFollowRedirect(final boolean followRedirect) {

      this.followRedirect = followRedirect;
      return this;
    }

    /**
     * Set metric factory for the client
     *
     * @param metricFactory metric factory
     * @return builder
     */
    @Deprecated //it is not in use
    public Builder setMetricFactory(final MetricFactory metricFactory) {

      this.metricFactory = metricFactory;
      return this;
    }

    /**
     * Set marshalling strategy default for the request builder
     *
     * @param marshallingStrategy marshalling strategy
     */
    public Builder setMarshallingStrategy(final MarshallingStrategy marshallingStrategy) {

      this.marshallingStrategy = Objects.requireNonNull(marshallingStrategy, "marshallingStrategy may not be null");
      return this;
    }

    /**
     * Set chunked file chunk size
     *
     * @param chunkedFileChunkSize chunked file chunk size
     * @return builder
     */
    public Builder setChuckedFileChuckSize(final int chunkedFileChunkSize) {

      this.chunkedFileChunkSize = chunkedFileChunkSize;
      return this;
    }

    /**
     * Creates new HttpClient from the configuration set
     *
     * @return new HttpClient instance
     */
    public HttpClient build() {

      final DefaultAsyncHttpClientConfig.Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder().
        setConnectTimeout(connectionTimeout).
        setMaxRequestRetry(retries).
        setRequestTimeout(requestTimeout).
        setReadTimeout(readTimeout).
        setCompressionEnforced(compressionEnforced).
        setDisableUrlEncodingForBoundRequests(disableUrlEncoding).
        setEventLoopGroup(EventLoopGroupHolder.GROUP).
        setNettyTimer(NettyTimerHolder.TIMER).
        setMaxConnectionsPerHost(maxConnectionsPerHost).
        setMaxConnections(maxTotalConnections).setSoReuseAddress(true).setTcpNoDelay(true).
        setChunkedFileChunkSize(chunkedFileChunkSize).
        setFollowRedirect(followRedirect);

      return new HttpClient(asyncHttpClient(configBuilder), responseMaxSize, marshallingStrategy);
    }
  }

  private static class NettyTimerHolder {
    private static final Timer TIMER = new HashedWheelTimer();
  }

  private static class EventLoopGroupHolder {
    private static final EventLoopGroup GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
  }
}
