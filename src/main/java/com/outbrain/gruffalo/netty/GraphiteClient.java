package com.outbrain.gruffalo.netty;

/**
 * @author Eran Harel
 */
public interface GraphiteClient {

  /**
   * Connects to the graphite relay
   */
  public void connect();

  public void publishMetrics(String metrics);

  /**
   * Notifies the client that the incoming requests are suspended due to slow writes
   */
  public void onPushBack();
}
