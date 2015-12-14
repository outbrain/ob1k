package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/16/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface ChooseContextPathPhase {
  ChooseServiceCreationTypePhase setContextPath(String contextPath, String appName);
  ChooseServiceCreationTypePhase setContextPath(String contextPath);
}
