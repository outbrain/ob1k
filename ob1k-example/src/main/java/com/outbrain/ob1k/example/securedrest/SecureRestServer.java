package com.outbrain.ob1k.example.securedrest;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.example.rest.server.endpoints.UsersService;
import com.outbrain.ob1k.example.securedrest.security.UserPassEqualAuthenticator;
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor;
import com.outbrain.ob1k.security.server.HttpBasicAuthenticationFilter;
import com.outbrain.ob1k.security.server.PathAssociations;
import com.outbrain.ob1k.security.server.PathAssociations.PathAssociationsBuilder;
import com.outbrain.ob1k.security.server.UserPasswordToken;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import com.outbrain.ob1k.server.build.ServiceBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Builds a new ob1k netty server with the security filter, defining all the endpoints
 * and starting on defined port.
 *
 * @author marenzon
 */
public class SecureRestServer {

  private static final Logger logger = LoggerFactory.getLogger(SecureRestServer.class);
  private static final int PORT = 8080;

  public static void main(final String[] args) {
    final Server server = buildServer(PORT);
    server.start();

    logger.info("** SecureRestServer Have Been Started On Port: {} **", PORT);
  }

  private static Server buildServer(final int port) {
    return ServerBuilder.newBuilder()
      .configurePorts(portProvider -> portProvider.setPort(port))
      .setContextPath("/api")
      .withServices(serviceProvider -> serviceProvider.defineService(new UsersService(), "/users", defineEndpoints()))
      .configureExtraParams(paramsProvider -> paramsProvider.setRequestTimeout(50, TimeUnit.MILLISECONDS))
      .build();
  }

  private static ServiceBindingProvider defineEndpoints() {
    HttpBasicAuthenticationFilter filter = createAuthFilter();
    return bindingProvider -> {
      bindingProvider.addEndpoint(HttpRequestMethodType.GET, "getAll", "/");
      bindingProvider.addEndpoint(HttpRequestMethodType.GET, "fetchUser", "/{id}", filter);
      bindingProvider.addEndpoint(HttpRequestMethodType.POST, "updateUser", "/{id}", filter);
      bindingProvider.addEndpoint(HttpRequestMethodType.DELETE, "deleteUser", "/{id}", filter);
      bindingProvider.addEndpoint(HttpRequestMethodType.PUT, "createUser", "/", filter);
    };
  }

  private static HttpBasicAuthenticationFilter createAuthFilter() {
    return new HttpBasicAuthenticationFilter(
      new AuthenticationCookieAesEncryptor(createKey()),
      createPathAssociations(),
      "myAppId",
      3600
    );
  }

  private static PathAssociations<UserPasswordToken> createPathAssociations() {
    return new PathAssociationsBuilder<UserPasswordToken>()
      .associate("/", new UserPassEqualAuthenticator())
      .build();
  }

  /**
   * Creates a random secret key.
   * <p>
   * In a real service this cannot be random - multiple instances of the save server must share a key
   * otherwise a specific instance will not be able to decrypt a cookie created by another instance.
   * </p>
   */
  private static byte[] createKey() {
    try {
      KeyGenerator generator = KeyGenerator.getInstance("AES");
      SecretKey key = generator.generateKey();
      return key.getEncoded();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error creating key", e);
    }
  }
}