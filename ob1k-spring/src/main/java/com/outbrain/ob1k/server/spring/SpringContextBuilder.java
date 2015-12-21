package com.outbrain.ob1k.server.spring;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: aronen
 * Date: 6/25/13
 * Time: 6:28 PM
 */
public class SpringContextBuilder<T extends SpringContextBuilder<T>> {
  public static final String DEFAULT_CONTEXT_PATH = "";

  private final List<CtxParams> subParams;
  private final boolean allowBeanOverrideByDefault;
  private CtxParams mainParams;
  private String activeProfiles;

  public SpringContextBuilder() {
    this(DEFAULT_CONTEXT_PATH);
  }

  public SpringContextBuilder(final String contextPath) {
    this(contextPath, true);
  }

  public SpringContextBuilder(final String contextPath, final boolean allowBeanOverrideByDefault) {
    System.setProperty("com.outbrain.web.context.path", contextPath);
    this.subParams = new ArrayList<>();
    this.allowBeanOverrideByDefault = allowBeanOverrideByDefault;
  }

  private static class CtxParams {
    final String key;
    final String path;
    private final boolean allowBeanOverride;

    private CtxParams(final String key, final String path, final boolean allowBeanOverride) {
      this.key = key;
      this.path = path;
      this.allowBeanOverride = allowBeanOverride;
    }
  }


  public T setMainContext(final String name, final String path) {

    return setMainContext(name,path, allowBeanOverrideByDefault);
  }

  public T setMainContext(final String name, final String path, final boolean allowBeanOverride) {
    mainParams = new CtxParams(name, path, allowBeanOverride);
    return self();
  }

  public T addSubContext(final String name, final String path) {
    return addSubContext(name,path, allowBeanOverrideByDefault);
  }

  public T addSubContext(final String name, final String path, final boolean allowBeanOverride) {
    subParams.add(new CtxParams(name, path, allowBeanOverride));
    return self();
  }
  
  public T setActiveProfiles(final String activeProfiles) {
    this.activeProfiles = activeProfiles;
    return self();
  }

  private AbstractApplicationContext createMainContext(final boolean allowBeanOverride) {
    final GenericXmlApplicationContext context = new GenericXmlApplicationContext();
    if (activeProfiles != null && !activeProfiles.isEmpty()) {
      context.getEnvironment().setActiveProfiles(activeProfiles);
    }
    context.setAllowBeanDefinitionOverriding(allowBeanOverride);
    context.load(mainParams.path);
    context.refresh();
    return context;
  }

  public AbstractApplicationContext createSubContext(final AbstractApplicationContext parent, final String path, final boolean allowBeanOverride) {
    final GenericXmlApplicationContext subContext = new GenericXmlApplicationContext();
    subContext.setParent(parent);
    subContext.setAllowBeanDefinitionOverriding(allowBeanOverride);
    subContext.load(path);
    subContext.refresh();
    return subContext;
  }

  public SpringBeanContext build() {
    final Map<String, AbstractApplicationContext> contexts = new HashMap<>();

    final AbstractApplicationContext mainCtx = createMainContext(mainParams.allowBeanOverride);
    contexts.put(mainParams.key, mainCtx);
    for(final CtxParams param: subParams) {
      final AbstractApplicationContext subCtx = createSubContext(mainCtx, param.path, param.allowBeanOverride);
      contexts.put(param.key, subCtx);
    }

    return new SpringBeanContext(contexts);
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }
}
