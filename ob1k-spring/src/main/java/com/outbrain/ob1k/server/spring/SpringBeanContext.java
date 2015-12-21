package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.BeanContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;

/**
 * User: aronen
 * Date: 6/25/13
 * Time: 6:50 PM
 */
public class SpringBeanContext implements BeanContext {
  private final Map<String, AbstractApplicationContext> contexts;

  public SpringBeanContext(final Map<String, AbstractApplicationContext> contexts) {
    this.contexts = contexts;
  }

  public <T> T getBean(final String ctxName, final Class<T> type) {
    return contexts.get(ctxName).getBean(type);
  }

  public <T> T getBean(final String ctxName, final String id, final Class<T> type) {
    return contexts.get(ctxName).getBean(id, type);
  }

  public boolean contextExists(final String ctxName) {
    return contexts.containsKey(ctxName);
  }
}
