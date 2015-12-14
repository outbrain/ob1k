package com.outbrain.ob1k.server;

/**
 * User: aronen
 * Date: 6/26/13
 * Time: 6:55 PM
 */
@Deprecated
public interface BeanContext {
  <T> T getBean(String ctxName, Class<T> type);
  <T> T getBean(String ctxName, String id, Class<T> type);
}
