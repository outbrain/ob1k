package com.outbrain.ob1k.security.server;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Represents a valid authentication cookie for users who have valid credentials.
 * This cookie is meant to be serialized into the response and then sent back by the user for each subsequent
 * request.
 */
class AuthenticationCookie {

  public final static String DELIMITER = ";";
  public static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.basicDateTime();

  private final String username;
  private final DateTime creationTime;
  private final String appId;
  private final String authenticatorId;

  public AuthenticationCookie(final String username,
                              final DateTime creationTime,
                              final String appId,
                              final String authenticatorId) {
    this.username = username;
    this.creationTime = creationTime;
    this.appId = appId;
    this.authenticatorId = authenticatorId;
  }

  /**
   * Converted the given delimited string into an Authentication cookie.
   * Expected a string in the format username;creationTime;appId;authenticatorId
   */
  public static AuthenticationCookie fromDelimitedString(final String delimitedString) {
    Preconditions.checkArgument(delimitedString != null && delimitedString.length() > 0,
                                "delimitedString cannot be empty");

    final String[] cookieElements = delimitedString.split(DELIMITER);
    Preconditions.checkArgument(cookieElements.length == 4, "delimitedString should contain exactly 4 elements");

    return new AuthenticationCookie(
      cookieElements[0],
      DateTime.parse(cookieElements[1], DATE_TIME_FORMATTER),
      cookieElements[2],
      cookieElements[3]);
  }

  /**
   * @return this AuthenticationCookie as a delimited string in the format
   * username;creationTime;appId;authenticatorId
   */
  public String toDelimitedString() {
    return
      username + DELIMITER +
        creationTime.toString(DATE_TIME_FORMATTER) + DELIMITER +
        appId + DELIMITER +
        authenticatorId;
  }

  public String getUsername() {
    return username;
  }

  public DateTime getCreationTime() {
    return creationTime;
  }

  public String getAppId() {
    return appId;
  }

  public String getAuthenticatorId() {
    return authenticatorId;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("username", username)
      .append("creationTime", creationTime)
      .append("appId", appId)
      .append("authenticatorId", authenticatorId)
      .toString();
  }
}
