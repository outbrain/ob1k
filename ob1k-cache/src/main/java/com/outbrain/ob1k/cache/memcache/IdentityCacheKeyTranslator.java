package com.outbrain.ob1k.cache.memcache;

/**
 * @author Eran Harel
 */
public class IdentityCacheKeyTranslator implements CacheKeyTranslator<String> {

  public static final CacheKeyTranslator<String> INSTANCE = new IdentityCacheKeyTranslator();

  private IdentityCacheKeyTranslator() {}

  @Override
  public String translateKey(final String key) {
    return key;
  }
}
