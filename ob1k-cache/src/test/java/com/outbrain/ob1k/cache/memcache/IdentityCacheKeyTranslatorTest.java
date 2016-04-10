package com.outbrain.ob1k.cache.memcache;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Eran Harel
 */
public class IdentityCacheKeyTranslatorTest {

  @Test
  public void testTranslate() {
    final String expectedKey = UUID.randomUUID().toString();
    final String translatedKey = IdentityCacheKeyTranslator.getInstance().translateKey(expectedKey);
    Assert.assertEquals(expectedKey,translatedKey);
  }
}
