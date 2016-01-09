package com.outbrain.ob1k.http.utils;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author marenzon
 */
public class UrlUtils {

  private final static URLCodec urlCodec = new URLCodec();

  /**
   * Encodes value to be url valid (e.g. query param)
   *
   * @param value to encode
   * @return encoded value
   * @throws EncoderException
   */
  public static String encode(final String value) throws EncoderException {

    return urlCodec.encode(value);
  }

  /**
   * Replaces path param in uri to a valid, encoded value
   *
   * @param url url to replace in (path params should be in the uri within curly braces)
   * @param param name of the path param (not inside curly braces)
   * @param value value to encode
   * @return url with path param replaced to the value
   * @throws EncoderException
   */
  public static String replacePathParam(final String url, final String param, final String value) throws EncoderException {

    final String pathParam = param.startsWith("{") ? param : "{" + param + "}";
    final String encodedValue = encode(value);
    return StringUtils.replace(url, pathParam, encodedValue);
  }

  /**
   * Extracts all path params in url
   *
   * @param url url to find path params in
   * @return list of path params
   */
  public static List<String> extractPathParams(final String url) {

    final List<String> pathParams = new ArrayList<>();
    int index = url.indexOf('{');

    while (index >= 0) {

      final int endIndex = url.indexOf('}', index);
      final String pathParam = url.substring(index + 1, endIndex);
      pathParams.add(pathParam);
      index = url.indexOf('{', endIndex);
    }

    return pathParams;
  }
}
