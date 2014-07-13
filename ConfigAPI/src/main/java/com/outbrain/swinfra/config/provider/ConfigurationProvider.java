package com.outbrain.swinfra.config.provider;

import java.util.Map;

/**
 * Implement this API to be able to provide configuration properties to our projects ;)
 *
 * @author Eran Harel
 */
public interface ConfigurationProvider {
  public Map<Object, Object> getConfiguration();
}
