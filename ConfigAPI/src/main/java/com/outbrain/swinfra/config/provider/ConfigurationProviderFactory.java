package com.outbrain.swinfra.config.provider;

/**
 * An instance of this class will be used to create a {@link ConfigurationProvider}.
 *
 * @author Eran Harel
 */
public interface ConfigurationProviderFactory {
  public ConfigurationProvider create();
}
