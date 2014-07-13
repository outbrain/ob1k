package com.outbrain.swinfra.config.provider;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SystemPropertyBasedConfigurationSelectorTest {

  @Test
  public void testGetConfiguration_happyPath() throws Exception {
    String selectionKey = "com.outbrain.swinfra.config.provider.SystemPropertyBasedConfigurationSelectorTest.impl";
    System.setProperty(selectionKey, MyConfigurationProvider.class.getName());
    Map<Object, Object> configuration = new SystemPropertyBasedConfigurationSelector(selectionKey).getConfiguration();

    assertEquals("retrieved configuration", MyConfigurationProvider.config, configuration);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetConfiguration_noKey_should_fail() throws Exception {
    new SystemPropertyBasedConfigurationSelector(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetConfiguration_noSystemValue_should_fail() throws Exception {
    new SystemPropertyBasedConfigurationSelector("no.such.system.property.should.exist");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetConfiguration_noConfigClass_should_fail() throws Exception {
    String selectionKey = "com.outbrain.swinfra.config.provider.SystemPropertyBasedConfigurationSelectorTest.impl";
    System.setProperty(selectionKey, "NoSuchClassShouldExist");
    new SystemPropertyBasedConfigurationSelector(selectionKey);
  }

  public static class MyConfigurationProvider implements ConfigurationProvider {

    private static final Map<Object, Object> config = new HashMap<>();
    static {
      config.put("foo", "bar");
    }

    @Override
    public Map<Object, Object> getConfiguration() {
      return config;
    }
  }
}