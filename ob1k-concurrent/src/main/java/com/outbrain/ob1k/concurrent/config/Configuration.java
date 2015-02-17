package com.outbrain.ob1k.concurrent.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by aronen on 2/3/15.
 *
 * basic configuration for the behavior of the futures.
 * the properties are either loaded from a properties file called ob1k-concurrent.properties that resides in the root of a resource directory
 * or a set of default values are used.
 */
public class Configuration {
  private static final Map<String, String> configuration;

  public static final String OB1K_THREAD_POOL_CORE_SIZE = "ob1k.threadPool.coreSize";
  public static final String OB1K_THREAD_POOL_MAX_SIZE = "ob1k.threadPool.maxSize";
  public static final String OB1K_SCHEDULER_CORE_SIZE = "ob1k.scheduler.coreSize";
  public static final String OB1K_DEFAULT_LAZY = "ob1k.default.lazy";

  static {
    configuration = new HashMap<>();
    try {
      final InputStream configResourceFile = Configuration.class.getResourceAsStream("ob1k-concurrent.properties");
      if (configResourceFile != null) {
        final Properties props = new Properties();
        props.load(configResourceFile);

        final Set<String> keys = props.stringPropertyNames();
        for (final String key : keys) {
          configuration.put(key, props.getProperty(key));
        }
      }
    } catch (final Exception e) {
      // no/corrupted config file. use default values.
    }

    if (!configuration.containsKey(OB1K_THREAD_POOL_CORE_SIZE)) {
      configuration.put(OB1K_THREAD_POOL_CORE_SIZE, "50");
    }

    if (!configuration.containsKey(OB1K_THREAD_POOL_MAX_SIZE)) {
      configuration.put(OB1K_THREAD_POOL_MAX_SIZE, "100");
    }

    if (!configuration.containsKey(OB1K_SCHEDULER_CORE_SIZE)) {
      configuration.put(OB1K_SCHEDULER_CORE_SIZE, "10");
    }

    if (!configuration.containsKey(OB1K_DEFAULT_LAZY)) {
      configuration.put(OB1K_DEFAULT_LAZY, "false");
    }
  }

  public static String getProperty(final String key) {
    return configuration.get(key);
  }

  public static int getExecutorCoreSize() {
    return Integer.parseInt(configuration.get(OB1K_THREAD_POOL_CORE_SIZE));
  }

  public static int getExecutorMaxSize() {
    return Integer.parseInt(configuration.get(OB1K_THREAD_POOL_MAX_SIZE));
  }

  public static int getSchedulerCoreSize() {
    return Integer.parseInt(configuration.get(OB1K_SCHEDULER_CORE_SIZE));
  }

  public static boolean isDefaultLazy() {
    return Boolean.parseBoolean(configuration.get(OB1K_DEFAULT_LAZY));
  }
}
