package com.outbrain.ob1k.example.securedrest;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.example.rest.server.endpoints.UsersService;
import com.outbrain.ob1k.security.ldap.LdapCredentialsAuthenticator;
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor;
import com.outbrain.ob1k.security.server.HttpBasicAuthenticationFilter;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import com.outbrain.ob1k.server.build.ServiceBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

/**
 * Builds a new ob1k netty server with a LDAP security filter, defining all the endpoints
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
      .build();
  }

  private static ServiceBindingProvider defineEndpoints() {
    final HttpBasicAuthenticationFilter filter = createAuthFilter();
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
      Lists.newArrayList(new LdapCredentialsAuthenticator("ldap", 389, "ou=People,dc=outbrain,dc=com")),
      "myAppId",
      3600
    );
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
      final KeyGenerator generator = KeyGenerator.getInstance("AES");
      final SecretKey key = generator.generateKey();
      return key.getEncoded();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Error creating key", e);
    }
  }
}