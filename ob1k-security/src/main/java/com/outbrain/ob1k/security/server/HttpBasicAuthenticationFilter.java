package com.outbrain.ob1k.security.server;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.server.ctx.AsyncServerRequestContext;
import com.outbrain.ob1k.server.netty.ResponseBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static io.netty.handler.codec.http.HttpHeaders.Names.WWW_AUTHENTICATE;

public class HttpBasicAuthenticationFilter implements AsyncFilter<Response, AsyncServerRequestContext> {

  private final HttpBasicAccessAuthenticator httpAccessAuthenticator;

  public HttpBasicAuthenticationFilter(final AuthenticationCookieEncryptor authenticationCookieEncryptor,
                                       final List<CredentialsAuthenticator<UserPasswordToken>> authenticators,
                                       final String appId,
                                       final int sessionMaxTimeSeconds) {
    this.httpAccessAuthenticator = new HttpBasicAccessAuthenticator(authenticationCookieEncryptor,
                                                                    authenticators,
                                                                    appId,
                                                                    sessionMaxTimeSeconds);
  }

  @Override
  public ComposableFuture<Response> handleAsync(final AsyncServerRequestContext ctx) {
    return httpAccessAuthenticator.authenticate(ctx.getRequest())
      .continueWith(new FutureResultHandler<String, Response>() {
        @Override
        public ComposableFuture<Response> handle(final Try<String> result) {
          if (result.isSuccess() && result.getValue() != null) {
            final String authenticatorId = result.getValue();
            return handleAuthorizedAsyncRequest(ctx, authenticatorId);
          } else {
            return handleUnauthorizedAsyncRequest(ctx);
          }
        }
      });
  }

  private ComposableFuture<Response> handleUnauthorizedAsyncRequest(final AsyncServerRequestContext ctx) {
    final Response response =
      httpAccessAuthenticator.createUnauthorizedResponse(extractRealm(ctx));
    return ComposableFutures.fromValue(response);
  }

  private ComposableFuture<Response> handleAuthorizedAsyncRequest(final AsyncServerRequestContext ctx,
                                                                  final String authenticatorId) {
    final String username = httpAccessAuthenticator.extractUsername(ctx.getRequest());
    return ctx.invokeAsync().continueOnSuccess(new FutureSuccessHandler<Object, Response>() {
      @Override
      public ComposableFuture<Response> handle(final Object result) {
        final Response response =
          httpAccessAuthenticator.createAuthorizedResponse(username, authenticatorId, result);
        return ComposableFutures.fromValue(response);
      }
    });
  }

  private String extractRealm(final AsyncServerRequestContext ctx) {
    return ctx.getRequest().getPath();
  }

  /**
   * Provides helper method for implementing HTTP Basic Authentication as described in RFC 2617
   *
   * @see <a href="https://www.ietf.org/rfc/rfc2617.txt">RFC 2617</a>
   */
  static class HttpBasicAccessAuthenticator {

    private final static Logger logger = LoggerFactory.getLogger(HttpBasicAccessAuthenticator.class);

    public static final String SESSION_COOKIE_HEADER = "ob1k-session";

    private final AuthenticationCookieEncryptor authenticationCookieEncryptor;
    private final List<CredentialsAuthenticator<UserPasswordToken>> authenticators;
    private final String appId;
    private final int sessionMaxTimeSeconds;
    private final BasicAuthenticationHeaderParser headerParser = new BasicAuthenticationHeaderParser();

    public HttpBasicAccessAuthenticator(final AuthenticationCookieEncryptor authenticationCookieEncryptor,
                                        final List<CredentialsAuthenticator<UserPasswordToken>> authenticators,
                                        final String appId,
                                        final int sessionMaxTimeSeconds) {
      this.authenticationCookieEncryptor = authenticationCookieEncryptor;
      this.authenticators = authenticators;
      this.appId = appId;
      this.sessionMaxTimeSeconds = sessionMaxTimeSeconds;
    }

    /**
     * Returns the authenticator id iff the given request is authenticated.
     * An authenticated request must includes a valid {@code AuthenticationCookie}
     *
     * @return The id of the {@code CredentialsAuthenticator} this request was authenticated with, and null if the
     * request is not authenticated
     * @see AuthenticationCookie
     */
    public ComposableFuture<String> authenticate(final Request request) {
      final AuthenticationCookie authenticationCookie = extractCookieElements(request);
      if (isValidCookie(authenticationCookie) && isAuthenticated(authenticationCookie)) {
        return ComposableFutures.fromValue(authenticationCookie.getAuthenticatorId());
      } else {
        return authenticateCredentials(request);
      }
    }

    /*
    Returns true if the cookie contains an authenticatorId that also exists in this filter's list of authenticators
     */
    private boolean isAuthenticated(final AuthenticationCookie cookie) {
      for (final CredentialsAuthenticator authenticator : authenticators) {
        if (StringUtils.equals(cookie.getAuthenticatorId(), authenticator.getId())) return true;
      }
      return false;
    }

    private boolean isValidCookie(final AuthenticationCookie authenticationCookie) {
      return authenticationCookie != null &&
        isValid(authenticationCookie.getCreationTime()) &&
        StringUtils.equals(authenticationCookie.getAppId(), this.appId);
    }

    private ComposableFuture<String> authenticateCredentials(final Request request) {
      final Credentials<UserPasswordToken> credentials = headerParser.extractCredentials(request);
      if (credentials != null) return authenticate(credentials, request.getPath());
      else return ComposableFutures.fromNull();
    }

    private ComposableFuture<String> authenticate(final Credentials<UserPasswordToken> credentials, final String path) {
      //Send the authentication requests, receiveing a map of AuthenticatorId -> Authentication Result
      final ComposableFuture<Map<String, Boolean>> authenticationResults = sendAuthenticationRequests(credentials);

      //Find the Authenticator that returned successfully authenticated the request
      return findAuthenticator(authenticationResults);
    }

    private ComposableFuture<Map<String, Boolean>> sendAuthenticationRequests(final Credentials<UserPasswordToken> credentials) {
      final Map<String, ComposableFuture<Boolean>> authenticationRequests = Maps.newHashMap();
      for (final CredentialsAuthenticator<UserPasswordToken> authenticator : authenticators) {
        authenticationRequests.put(authenticator.getId(), authenticator.authenticate(credentials));
      }

      return ComposableFutures.all(false, authenticationRequests);
    }

    private ComposableFuture<String> findAuthenticator(final ComposableFuture<Map<String, Boolean>> authenticationResults) {
      return authenticationResults.continueOnSuccess(new FutureSuccessHandler<Map<String, Boolean>, String>() {
        @Override
        public ComposableFuture<String> handle(final Map<String, Boolean> authenticationResults) {
          String authenticatorId = null;
          for (final Entry<String, Boolean> authenticationResult : authenticationResults.entrySet()) {
            if (authenticationResult.getValue()) authenticatorId = authenticationResult.getKey();
          }
          return ComposableFutures.fromValue(authenticatorId);
        }
      });
    }

    private boolean isValid(final DateTime creationTime) {
      final DateTime now = DateTime.now(creationTime.getZone());
      final int seconds = Seconds.secondsBetween(creationTime, now).getSeconds();
      return seconds <= sessionMaxTimeSeconds;
    }

    /**
     * Extracts the username from the cookie the request contains.
     *
     * @return the username, or null if the request is not authenticated
     */
    public String extractUsername(final Request request) {
      final AuthenticationCookie authenticationCookie = extractCookieElements(request);
      if (authenticationCookie == null) return null;

      return authenticationCookie.getUsername();
    }

    private AuthenticationCookie extractCookieElements(final Request request) {
      final String encodedCookie = request.getHeader(SESSION_COOKIE_HEADER);
      if (encodedCookie == null || encodedCookie.length() == 0) return null;

      try {
        return authenticationCookieEncryptor.decrypt(encodedCookie);
      } catch (final Exception e) {
        logger.warn("Error decrypting cookie for request " + request);
        return null;
      }
    }

    /**
     * Creates a response that is marked as OK, containing the provided message.
     * The response will also contain the authentication cookie that is composed from the given username and authenticator id
     *
     * @return the same given response, with the cookie added to it
     */
    Response createAuthorizedResponse(final String username,
                                      final String authenticatorId,
                                      final Object message) {

      final AuthenticationCookie authenticationCookie = new AuthenticationCookie(username,
                                                                                 DateTime.now(),
                                                                                 appId,
                                                                                 authenticatorId);
      final String cookieValue = authenticationCookieEncryptor.encrypt(authenticationCookie);
      return ResponseBuilder
        .ok()
        .withMessage(message)
        .addCookie(SESSION_COOKIE_HEADER, cookieValue)
        .build();
    }

    /**
     * Creates a response that is marked as unauthorized
     *
     * @param realm the relevant authentication realm for the unauthorized request. The realm should provide
     *              the client a hint as to which credentials are relevant
     */
    Response createUnauthorizedResponse(final String realm) {
      return ResponseBuilder
        .fromStatus(HttpResponseStatus.UNAUTHORIZED)
        .addHeader(WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
        .build();
    }

  }
}
