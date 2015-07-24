package com.outbrain.ob1k.db;


import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory;
import com.github.mauricio.async.db.pool.PoolConfiguration;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import scala.Option;

/**
 * Builds a MySql {@link DbConnectionPool}.
 *
 * @author Eran Harel
 */
public class MySqlConnectionPoolBuilder {

  private final String host;
  private final int port;
  private final String username;
  private MySQLConnectionFactory connectionFactory;
  private Option<String> password = Option.empty();
  private Option<String> database = Option.empty();
  private long connectTimeoutSeconds = 2;
  private int maxConnections = 10;
  private Option<Integer> maxQueueSize = Option.empty();

  private long maxIdleTimeMs = 15 * 60 * 1000;
  private long validationIntervalMs = 30 * 1000;

  private MetricFactory metricFactory;

  private MySqlConnectionPoolBuilder(final String host, final  int port, final String username) {
    this.host = host;
    this.port = port;
    this.username = username;
  }

  private MySqlConnectionPoolBuilder(final MySQLConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
    this.host = null;
    this.port = -1;
    this.username = null;
  }

  public static MySqlConnectionPoolBuilder newBuilder(final MySQLConnectionFactory connectionFactory) {
    return new MySqlConnectionPoolBuilder(connectionFactory);
  }

  /**
   * @param host DB host
   * @param username user name for login.
   * @param port DB port
   */
  public static MySqlConnectionPoolBuilder newBuilder(final String host, final int port, final String username) {
    return new MySqlConnectionPoolBuilder(host, port, username);
  }

  /**
   * Warning - Use only when the connection string is in host:port format without schema and other parameter.
   * example: mydb.company.com:3308
   *
   * @param connectionString host:port
   * @param username         user name for login.
   */
  public static MySqlConnectionPoolBuilder newBuilder(final String connectionString, final String username) {
    final String[] arr = connectionString.split(":");
    final String host = arr[0];
    final int port = Integer.parseInt(arr[1]);
    return newBuilder(host, port, username);
  }


  public MySqlConnectionPoolBuilder forDatabase(final String database) {
    this.database = Option.apply(database);
    return this;
  }

  public MySqlConnectionPoolBuilder password(final String password) {
    this.password = Option.apply(password);
    return this;
  }

  public MySqlConnectionPoolBuilder connectTimeoutSeconds(final long connectTimeoutSeconds) {
    this.connectTimeoutSeconds = connectTimeoutSeconds;
    return this;
  }

  public MySqlConnectionPoolBuilder maxConnections(final int maxConnections) {
    this.maxConnections = maxConnections;
    return this;
  }

  public MySqlConnectionPoolBuilder maxQueueSize(final Integer maxQueueSize) {
    this.maxQueueSize = Option.apply(maxQueueSize);
    return this;
  }

  public MySqlConnectionPoolBuilder maxIdleTimeMs(final Integer maxIdleTimeMs) {
    this.maxIdleTimeMs = maxIdleTimeMs;
    return this;
  }

  public MySqlConnectionPoolBuilder validationIntervalMs(final Integer validationIntervalMs) {
    this.validationIntervalMs = validationIntervalMs;
    return this;
  }

  public MySqlConnectionPoolBuilder withMetrics(final MetricFactory metricFactory) {
    this.metricFactory = metricFactory;
    return this;
  }

  public DbConnectionPool build() {
    final MySQLConnectionFactory connFactory = connectionFactory == null ?
            new MySQLConnectionFactory(MySqlAsyncConnection.createConfiguration(host, port, database, username, password, connectTimeoutSeconds))
            : connectionFactory;
    int finalMaxQueueSize = maxQueueSize.isEmpty() ? maxConnections * 2 : maxQueueSize.get();
    final PoolConfiguration configuration = new PoolConfiguration(maxConnections, maxIdleTimeMs, finalMaxQueueSize, validationIntervalMs);

    return new MySqlConnectionPool(connFactory, configuration, metricFactory);
  }
}
