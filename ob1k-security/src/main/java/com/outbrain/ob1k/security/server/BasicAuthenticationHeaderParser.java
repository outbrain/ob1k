package com.outbrain.ob1k.security.server;

import com.ning.http.util.Base64;
import com.outbrain.ob1k.Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses an "Authorization" header of a HTTP request
 */
class BasicAuthenticationHeaderParser {

  private final static Logger logger = LoggerFactory.getLogger(BasicAuthenticationHeaderParser.class);

  public static final String BASIC_AUTHORIZATION_HEADER = "Authorization";
  public static final String BASIC_PREFIX = "Basic";

  /**
   * <p>
   * Extracts the credentials from the request by obtaining the Authorization header's value
   * and parsing it. Everything after the &quot;Basic&quot; prefix will be Base64 decoded and returns.
   * </p>
   * Examples:<br>
   * <pre>
   *   Authorization Header            Result
   *   ---------------------------------------------
   *   null                            null
   *   ""                              null
   *   "Basic unencoded_string"        null
   *   "Basic dXNlcjpwYXNzd29yZA=="    {@code new UsernamePasswordTuple("user", "password"}
   * </pre>
   *
   * @return the decoded credentials, or null if any error occured
   */
  public Credentials<UserPasswordToken> extractCredentials(final Request request) {
    final String basicAuthHeader = request.getHeader(BASIC_AUTHORIZATION_HEADER);
    if (StringUtils.isNotBlank(basicAuthHeader) && basicAuthHeader.startsWith(BASIC_PREFIX)) {
      return extractCredentials(basicAuthHeader);
    } else {
      return null;
    }
  }

  private Credentials<UserPasswordToken> extractCredentials(final String basicAuthHeader) {
    final String encodedCredentials = basicAuthHeader.substring(BASIC_PREFIX.length()).trim();
    if (StringUtils.isNotBlank(encodedCredentials)) {
      final String decodedCredentials = decode(encodedCredentials);

      if (StringUtils.isNotBlank(decodedCredentials)) {
        return splitCredentials(decodedCredentials);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  //Splits a string in the form "username:password" into the UsernamePasswordTuple
  private Credentials<UserPasswordToken> splitCredentials(final String credentials) {
    final String[] splitCredentials = credentials.split(":");
    if (splitCredentials.length != 2) {
      logger.error("Error splitting credentials {}", credentials);
      return null;
    } else {
      final UserPasswordToken userPassTuple = new UserPasswordToken(splitCredentials[0],
                                                                            splitCredentials[1].toCharArray());
      return new HttpBasicCredentials(userPassTuple);
    }
  }

  private String decode(final String encodedCredentials) {
    try {
      return new String(Base64.decode(encodedCredentials));
    } catch (final Exception e) {
      logger.error("Error decoding credentials " + encodedCredentials, e);
      return null;
    }
  }

}
