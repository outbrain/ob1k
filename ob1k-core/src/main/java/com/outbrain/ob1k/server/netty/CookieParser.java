package com.outbrain.ob1k.server.netty;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * This class parses cookies from within the value of a "Cookie" header
 *
 * @author guymarom
 */
public class CookieParser {

  private static final String COOKIE_DELIMITER = ";";
  private static final String NAME_VALUE_DELIMITER = "=";

  public Map<String, String> parse(final String headerValue) {
    if (StringUtils.isNotBlank(headerValue)) {
      return doParse(headerValue);
    } else {
      return Maps.newHashMap();
    }
  }

  /**
   * Parses a header value string into a map of (name -> value) pairs while ignoring invalid cookies
   * <p/>
   * "name1=val1" -> ("name1" -> "val1")
   * "name1=val1;name2=val2" -> ("name1" -> "val1", "name2" -> "val2")
   * "name1=val1;name2=val2=what??" -> ("name1" -> "val1")
   */
  private Map<String, String> doParse(final String headerValue) {
    final Map<String, String> result = Maps.newHashMap();
    final String[] cookies = headerValue.split(COOKIE_DELIMITER);

    for (final String cookie : cookies) {
      final String[] cookieNameValue = cookie.split(NAME_VALUE_DELIMITER);
      if (cookieNameValue.length == 2) {
        final String cookieName = cookieNameValue[0];
        final String cookieValue = cookieNameValue[1];

        if (StringUtils.isNotBlank(cookieName) && StringUtils.isNotBlank(cookieValue)) {
          result.put(cookieNameValue[0], cookieNameValue[1]);
        }
      }
    }

    return result;
  }

}
