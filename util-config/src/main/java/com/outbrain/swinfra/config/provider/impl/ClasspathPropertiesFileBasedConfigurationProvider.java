package com.outbrain.swinfra.config.provider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.outbrain.swinfra.config.provider.ConfigurationProvider;

/**
 * A {@link com.outbrain.swinfra.config.provider.ConfigurationProvider} that fetches the configuration from a class path resource.
 *
 * @author Eran Harel
 */
public class ClasspathPropertiesFileBasedConfigurationProvider implements ConfigurationProvider {

  private static final Logger log = LoggerFactory.getLogger(ClasspathPropertiesFileBasedConfigurationProvider.class);

  private final Properties properties = new Properties();

  public ClasspathPropertiesFileBasedConfigurationProvider(String resourceFilePath) throws IOException {
    if (null == resourceFilePath) {
      throw new IllegalArgumentException("Configuration file path must be provided");
    }

    log.info("loading resource {}", resourceFilePath);
    InputStream configFileStream = getClass().getClassLoader().getResourceAsStream(resourceFilePath);
    if (null == configFileStream) {
      throw new IllegalArgumentException("Configuration file doesn't exist - " + resourceFilePath);
    }

    properties.load(configFileStream);
  }

  @Override
  public Map<Object, Object> getConfiguration() {
    return Collections.unmodifiableMap(properties);
  }
}
