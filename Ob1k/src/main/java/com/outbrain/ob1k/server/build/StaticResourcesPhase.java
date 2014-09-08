package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.Server;

/**
 * Created by aronen on 7/16/14.
 */
public interface StaticResourcesPhase {
  StaticResourcesPhase addStaticResource(final String mapping, final String location);
  StaticResourcesPhase addStaticPath(final String path);
  StaticResourcesPhase addStaticMapping(final String virtualPath, final String realPath);
}
