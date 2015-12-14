package com.outbrain.ob1k.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: aronen
 * Date: 7/29/13
 * Time: 5:39 PM
 */
public class StaticPathResolver {
  private final Map<String, String> fileMappings;
  private final List<String> folders;
  private final Map<String, String> staticResources;

  private final String contextPath;


  public StaticPathResolver(String contextPath, Set<String> folders, Map<String, String> fileMappings, Map<String, String> staticResources) {
    this.fileMappings = new HashMap<>();
    this.staticResources = new HashMap<>();

    this.contextPath = contextPath;
    String trimmedContextPath = trimSlash(contextPath);

    this.folders = new ArrayList<>();
    for (String folder : folders) {
      String fullPath = getTrimmedFullStaticPath(trimmedContextPath, folder);
      this.folders.add(fullPath);
    }

    for (String mappingKey : fileMappings.keySet()) {
      String newKey = trimmedContextPath.isEmpty() ? "/" + trimSlash(mappingKey) : "/" + trimmedContextPath + "/" + trimSlash(mappingKey);
      String value = fileMappings.get(mappingKey);
      this.fileMappings.put(newKey, value);
    }

    for (final Map.Entry<String, String> staticResource : staticResources.entrySet()) {
      final String mapping = getTrimmedFullStaticPath(trimmedContextPath, staticResource.getKey());
      final String location = getTrimmedFullStaticPath(trimmedContextPath, staticResource.getValue());
      this.staticResources.put(mapping, location);
    }
  }

  private String getTrimmedFullStaticPath(String trimmedContextPath, String folder) {
    String trimmedStaticPath = trimSlash(folder);
    return trimmedContextPath.isEmpty() ? "/" + trimmedStaticPath : "/" + trimmedContextPath + "/" + trimmedStaticPath;
  }

  public boolean isStaticPath(String path) {
    for (String folder : folders) {
      if (path.startsWith(folder))
        return true;
    }

    for (final Map.Entry<String, String> staticResource : staticResources.entrySet()) {
      if (path.startsWith(staticResource.getKey())) {
        return true;
      }
    }

    return fileMappings.keySet().contains(path);
  }

  private static String trimSlash(String path) {
    if (path.startsWith("/"))
      path = path.substring(1);

    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1);

    return path;
  }

  public String getRelativePath(String uri) {
    for (final Map.Entry<String, String> staticResource : staticResources.entrySet()) {
      if (uri.startsWith(staticResource.getKey())) {
        uri = uri.replaceFirst(staticResource.getKey(), staticResource.getValue());
        break;
      }
    }

    for (String folder : folders) {
      if (uri.startsWith(folder)) {
        return uri.substring(contextPath.length());
      }
    }

    return fileMappings.get(uri);
  }
}
