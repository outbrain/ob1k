package com.outbrain.ob1k.db.experimental.springsupport;

import com.outbrain.ob1k.db.DbConnectionPool;
import com.outbrain.ob1k.db.MySqlConnectionPoolBuilder;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring support for MySqlConnectionPool creation
 * @author Eran Harel
 */
public class MySqlConnectionPoolFactoryBean implements FactoryBean {

  private final MySqlConnectionPoolBuilder builder;

  public MySqlConnectionPoolFactoryBean(final String connectionString, final String username) {
    builder = MySqlConnectionPoolBuilder.newBuilder(connectionString, username);
  }

  @Override
  public Object getObject() throws Exception {
    return builder.build();
  }

  @Override
  public Class<?> getObjectType() {
    return DbConnectionPool.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setDatabase(final String database) {
    builder.forDatabase(database);
  }

  public void setPassword(final String password) {
    builder.password(password);
  }

  public void setConnectTimeout(final long connectTimeoutMilliSeconds) {
    builder.connectTimeout(connectTimeoutMilliSeconds);
  }

  public void setQueryTimeout(final long queryTimeoutMilliSeconds) {
    builder.queryTimeout(queryTimeoutMilliSeconds);
  }

  public void setMaxConnections(final int maxConnections) {
    builder.maxConnections(maxConnections);
  }

  public void setMaxQueueSize(final Integer maxQueueSize) {
    builder.maxQueueSize(maxQueueSize);
  }

  public void setMaxIdleTimeMs(final Integer maxIdleTimeMs) {
    builder.maxIdleTimeMs(maxIdleTimeMs);
  }

  public void setValidationIntervalMs(final Integer validationIntervalMs) {
    builder.validationIntervalMs(validationIntervalMs);
  }

  public void setMetricFactory(final MetricFactory metricFactory) {
    builder.withMetrics(metricFactory);
  }

}
