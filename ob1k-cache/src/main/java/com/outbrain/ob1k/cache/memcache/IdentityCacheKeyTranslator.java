package com.outbrain.ob1k.cache.memcache;

/**
 * @author Eran Harel
 */
public class IdentityCacheKeyTranslator implements CacheKeyTranslator<String> {

  private static final CacheKeyTranslator<String> INSTANCE = new IdentityCacheKeyTranslator();

  public static CacheKeyTranslator<String> getInstance() {
    return INSTANCE;
  }

  private IdentityCacheKeyTranslator() {}

  @Override
  public String translateKey(final String key) {
    return key;
  }
}
