package com.outbrain.ob1k.http.utils;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

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

    final String pathParam = new StringBuilder().append('{').append(param).append('}').toString();
    final String encodedValue = encode(value);
    return StringUtils.replace(url, pathParam, encodedValue);
  }
}
