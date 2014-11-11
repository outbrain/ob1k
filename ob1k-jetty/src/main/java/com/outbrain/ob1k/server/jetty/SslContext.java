package com.outbrain.ob1k.server.jetty;

/**
 * Time: 2/16/14 2:13 PM
 *
 * @author Eran Harel
 */
public interface SslContext {
  public int getSecurePort();

  public String getKeyManagerPassword();

  public String getKeyStorePassword();

  public String getKeyStorePath();
}
