package com.outbrain.ob1k.http;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

import com.outbrain.ob1k.http.marshalling.JacksonMarshallingStrategy;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import com.outbrain.ob1k.http.ning.NingRequestBuilder;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;

import java.io.Closeable;
import java.io.IOException;

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
  public static final int MAX_CONNECTIONS_PER_HOST = 100;
  public static final int MAX_TOTAL_CONNECTIONS = MAX_CONNECTIONS_PER_HOST * 2;

  private final AsyncHttpClient asyncHttpClient;
  private final MetricFactory metricFactory;
  private final MarshallingStrategy marshallingStrategy;
  private final long responseMaxSize;

  private HttpClient(final AsyncHttpClient asyncHttpClient, final MetricFactory metricFactory,
                     final long responseMaxSize, final MarshallingStrategy marshallingStrategy) {

    this.asyncHttpClient = asyncHttpClient;
    this.metricFactory = metricFactory;
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

    checkNotNull(url, "url may not be null");
    final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareGet(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http post request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder post(final String url) {

    checkNotNull(url, "url may not be null");
    final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder = asyncHttpClient.preparePost(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http put request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder put(final String url) {

    checkNotNull(url, "url may not be null");
    final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder = asyncHttpClient.preparePut(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http delete request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder delete(final String url) {

    checkNotNull(url, "url may not be null");
    final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareDelete(url);
    return createNewRequestBuilder(url, ningRequestBuilder);
  }

  /**
   * Http head request
   *
   * @param url url for the request
   * @return request builder
   */
  public RequestBuilder head(final String url) {

    checkNotNull(url, "url may not be null");
    final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder = asyncHttpClient.prepareHead(url);
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

  private NingRequestBuilder createNewRequestBuilder(final String url, final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder) {

    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory, responseMaxSize, marshallingStrategy);
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

    private MarshallingStrategy marshallingStrategy = new JacksonMarshallingStrategy();
    private MetricFactory metricFactory;
    private int connectionTimeout = HttpClient.CONNECTION_TIMEOUT;
    private int requestTimeout = HttpClient.REQUEST_TIMEOUT;
    private int retries = HttpClient.RETRIES;
    private int maxConnectionsPerHost = HttpClient.MAX_CONNECTIONS_PER_HOST;
    private int maxTotalConnections = HttpClient.MAX_TOTAL_CONNECTIONS;
    private boolean compressionEnforced;
    private boolean disableUrlEncoding;
    private boolean followRedirect;
    private boolean acceptAnySslCertificate;
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
     * Accepting all SSL certificates, even if they invalid
     * Note: NOT recommended to use, since it's may cause potential security issue.
     *
     * @param acceptAnySslCertificate accept any ssl certificate boolean
     * @see <a href="http://security.stackexchange.com/questions/22965/what-is-the-potential-impact-of-these-ssl-certificate-validation-vulnerabilities">ssl cetificate validation vulnerabilities</a>
     * @return builder
     */
    public Builder setAcceptAnySslCertificate(final boolean acceptAnySslCertificate) {

      this.acceptAnySslCertificate = acceptAnySslCertificate;
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

      this.marshallingStrategy = checkNotNull(marshallingStrategy, "marshallingStrategy may not be null");
      return this;
    }

    /**
     * Creates new HttpClient from the configuration set
     *
     * @return new HttpClient instance
     */
    public HttpClient build() {

      final AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().
              setConnectTimeout(connectionTimeout).
              setMaxRequestRetry(retries).
              setRequestTimeout(requestTimeout).
              setCompressionEnforced(compressionEnforced).
              setDisableUrlEncodingForBoundedRequests(disableUrlEncoding).
              setMaxConnectionsPerHost(maxConnectionsPerHost).
              setMaxConnections(maxTotalConnections).
              setAsyncHttpClientProviderConfig(NettyConfigHolder.INSTANCE).
              setFollowRedirect(followRedirect).
              setAcceptAnyCertificate(acceptAnySslCertificate).
              build();

      return new HttpClient(new AsyncHttpClient(config), metricFactory, responseMaxSize, marshallingStrategy);
    }
  }

  /**
   * A singleton of Netty's httpProvider config, so all the clients
   * will share the same thread pool for all the executions.
   *
   * @author aronen
   */
  private static class NettyConfigHolder {

    private static final NettyAsyncHttpProviderConfig INSTANCE = createConfig();

    private static NettyAsyncHttpProviderConfig createConfig() {

      final NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
      final NioClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory();

      nettyConfig.setSocketChannelFactory(channelFactory);
      nettyConfig.setChunkedFileChunkSize(CHUNKED_FILE_CHUNK_SIZE);

      final HashedWheelTimer timer = new HashedWheelTimer();
      timer.start();
      nettyConfig.setNettyTimer(timer);

      registerShutdownHook(channelFactory, timer);
      return nettyConfig;
    }

    private static void registerShutdownHook(final NioClientSocketChannelFactory channelFactory, final HashedWheelTimer hashedWheelTimer) {

      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          channelFactory.shutdown();
          channelFactory.releaseExternalResources();
          hashedWheelTimer.stop();
        }
      });
    }
  }
}