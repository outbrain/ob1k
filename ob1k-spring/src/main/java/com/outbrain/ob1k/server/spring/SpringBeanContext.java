package com.outbrain.ob1k.server.spring;

import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;

/**
 * User: aronen
 * Date: 6/25/13
 * Time: 6:50 PM
 */
public class SpringBeanContext {
  private final Map<String, AbstractApplicationContext> contexts;

  public SpringBeanContext(final Map<String, AbstractApplicationContext> contexts) {
    this.contexts = contexts;
  }

  public <T> T getBean(final String ctxName, final Class<T> type) {
    final AbstractApplicationContext context = contexts.get(ctxName);
    if (context == null) {
      throw new NullPointerException("Context not found for name '" + ctxName + "'");
    }
    return context.getBean(type);
  }

  public <T> T getBean(final String ctxName, final String id, final Class<T> type) {
    final AbstractApplicationContext context = contexts.get(ctxName);
    if (context == null) {
      throw new NullPointerException("Context not found for name '" + ctxName + "'");
    }
    return context.getBean(id, type);
  }

  public boolean contextExists(final String ctxName) {
    return contexts.containsKey(ctxName);
  }
}
