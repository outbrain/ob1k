package com.outbrain.ob1k.http;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

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

  private HttpClient(final AsyncHttpClient asyncHttpClient, final MetricFactory metricFactory) {

    this.asyncHttpClient = asyncHttpClient;
    this.metricFactory = metricFactory;
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
    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory);
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
    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory);
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
    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory);
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
    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory);
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
    return new NingRequestBuilder(asyncHttpClient, ningRequestBuilder, url, metricFactory);
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

    private MetricFactory metricFactory;
    private int connectionTimeout = HttpClient.CONNECTION_TIMEOUT;
    private int requestTimeout = HttpClient.REQUEST_TIMEOUT;
    private int retries = HttpClient.RETRIES;
    private int maxConnectionsPerHost = HttpClient.MAX_CONNECTIONS_PER_HOST;
    private int maxTotalConnections = HttpClient.MAX_TOTAL_CONNECTIONS;
    private boolean compressionEnforced = false;
    private boolean disableUrlEncoding = false;
    private boolean followRedirect = false;
    private boolean acceptAnySslCertificate = false;

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

    public Builder setConnectionTimeout(final int connectionTimeout) {

      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder setRequestTimeout(final int requestTimeout) {

      this.requestTimeout = requestTimeout;
      return this;
    }

    public Builder setCompressionEnforced(final boolean compressionEnforced) {

      this.compressionEnforced = compressionEnforced;
      return this;
    }

    public Builder setDisableUrlEncoding(final boolean disableUrlEncoding) {

      this.disableUrlEncoding = disableUrlEncoding;
      return this;
    }

    public Builder setFollowRedirect(final boolean followRedirect) {

      this.followRedirect = followRedirect;
      return this;
    }

    public Builder setMetricFactory(final MetricFactory metricFactory) {

      this.metricFactory = metricFactory;
      return this;
    }

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

      return new HttpClient(new AsyncHttpClient(config), metricFactory);
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