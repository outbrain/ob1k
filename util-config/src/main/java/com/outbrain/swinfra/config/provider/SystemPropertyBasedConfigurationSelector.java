package com.outbrain.swinfra.config.provider;

import java.util.Map;

/**
 * A selector for a {@link com.outbrain.swinfra.config.provider.ConfigurationProvider} based on a class name specified as a System property.
 *
 * @author Eran Harel
 */
public class SystemPropertyBasedConfigurationSelector implements ConfigurationProvider {

  public static final String CONFIGURATION_SELECTION_PROPERTY_KEY = "com.outbrain.swinfra.config.provider.impl.class";

  private final ConfigurationProvider configProvider;

  public SystemPropertyBasedConfigurationSelector() {
    this(CONFIGURATION_SELECTION_PROPERTY_KEY);
  }

  public SystemPropertyBasedConfigurationSelector(final String configSelectionPropertyKey) {
    if (null == configSelectionPropertyKey) {
      throw new IllegalArgumentException("configSelectionPropertyKey must not be null");
    }

    final String configProviderFactoryClass = System.getProperty(configSelectionPropertyKey);
    if (configProviderFactoryClass == null) {
      throw new IllegalArgumentException("No ConfigurationProviderFactory implementation class was provided under the system property '" + configSelectionPropertyKey +"'");
    }

    try {
      configProvider = (ConfigurationProvider) Class.forName(configProviderFactoryClass).newInstance();
    } catch (final ClassNotFoundException e) {
      throw new IllegalArgumentException("Configuration factory class was not found in classpath - " + configProviderFactoryClass, e);
    } catch (final InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException("Failed to instantiate configuration factory class - " + configProviderFactoryClass, e);
    }
  }

  @Override
  public Map<Object, Object> getConfiguration() {
    return configProvider.getConfiguration();
  }
}
