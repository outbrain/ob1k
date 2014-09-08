package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/16/14.
 */
public interface ChooseContextPathPhase {
  ChooseServiceCreationTypePhase setContextPath(String contextPath, String appName);
  ChooseServiceCreationTypePhase setContextPath(String contextPath);
}
