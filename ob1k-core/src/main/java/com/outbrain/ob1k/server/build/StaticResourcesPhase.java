package com.outbrain.ob1k.server.build;

/**
 * Created by aronen on 7/16/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface StaticResourcesPhase {
  StaticResourcesPhase addStaticResource(final String mapping, final String location);
  StaticResourcesPhase addStaticPath(final String path);
  StaticResourcesPhase addStaticMapping(final String virtualPath, final String realPath);
}
