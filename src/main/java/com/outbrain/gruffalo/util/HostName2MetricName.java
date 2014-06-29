package com.outbrain.gruffalo.util;

import java.util.regex.Pattern;

/**
 * Time: 8/13/13 10:00 AM
 *
 * @author Eran Harel
 */
public class HostName2MetricName {
  private static final Pattern REPLACED_CHARS = Pattern.compile("[\\.\\:]");

  public static String graphiteCompatibleHostPortName(String hostAndPort) {
    return REPLACED_CHARS.matcher(hostAndPort).replaceAll("_");
  }
}
