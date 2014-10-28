package com.outbrain.swinfra.config.provider.impl;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ClasspathPropertiesFileBasedConfigurationProviderTest {

  @Test
  public void testGetConfiguration_happyPath() throws Exception {
    final String key1 = "ClasspathPropertiesFileBasedConfigurationProviderTest.p1";
    final String key2 = "ClasspathPropertiesFileBasedConfigurationProviderTest.p2";

    Map<Object, Object> configuration = new ClasspathPropertiesFileBasedConfigurationProvider("ClasspathPropertiesFileBasedConfigurationProviderTest.properties").getConfiguration();
    assertEquals("ClasspathPropertiesFileBasedConfigurationProviderTest.p1", "v1", configuration.get(key1));
    assertEquals("ClasspathPropertiesFileBasedConfigurationProviderTest.p2", "v2", configuration.get(key2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetConfiguration_noResourceFileProvided_should_fail() throws Exception {
    new ClasspathPropertiesFileBasedConfigurationProvider(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetConfiguration_resourceMissing_should_fail() throws Exception {
    new ClasspathPropertiesFileBasedConfigurationProvider("no-such-file-exists");
  }
}