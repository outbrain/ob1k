package com.outbrain.ob1k.security.common;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor;
import com.outbrain.ob1k.security.server.CredentialsAuthenticator;
import com.outbrain.ob1k.security.server.HttpBasicAuthenticationFilter;
import com.outbrain.ob1k.security.server.UserPasswordToken;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.builder.ConfigureBuilder;
import com.outbrain.ob1k.server.builder.ConfigureBuilder.ConfigureBuilderSection;
import com.outbrain.ob1k.server.builder.ServerBuilder;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder.ServiceRegisterBuilderSection;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;

/**
 * Created by gmarom on 6/24/15
 */
public class TestServer {

  public final static String CONTEXT_PATH = "/app/";

  public static void main(final String[] args) throws IOException {
    final Server server = newServer();
    final InetSocketAddress address = server.start();
    System.out.println("Server started at " + address);
    System.out.println("Enter");
    System.in.read();
    server.stop();
  }

  public static Server newServer() {
    return ServerBuilder.newBuilder()
      .contextPath(CONTEXT_PATH)
      .configure(createPortsProvider())
      .service(createServices())
      .build();
  }

  private static ServiceRegisterBuilderSection createServices() {
    final ServiceFilter securityFilter = createAuthFilter();

    return new ServiceRegisterBuilderSection() {
      @Override
      public void apply(final ServiceRegisterBuilder builder) {
        builder.register(new UnsecureServiceImpl(), UnsecureService.class.getSimpleName())
                .register(new SecureServiceImpl(), SecureService.class.getSimpleName(), securityFilter);
      }
    };
  }

  private static HttpBasicAuthenticationFilter createAuthFilter() {
    return new HttpBasicAuthenticationFilter(
      new AuthenticationCookieAesEncryptor(createKey()),
      Lists.<CredentialsAuthenticator<UserPasswordToken>>newArrayList(new UserPassEqualAuthenticator()),
      "myAppId",
      3600
    );
  }

  private static byte[] createKey() {
    try {
      final KeyGenerator generator = KeyGenerator.getInstance("AES");
      final SecretKey key = generator.generateKey();
      return key.getEncoded();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Error creating key", e);
    }
  }

  private static ConfigureBuilderSection createPortsProvider() {
    return new ConfigureBuilderSection() {
      @Override
      public void apply(final ConfigureBuilder builder) {
        builder.useRandomPort();
      }
    };
  }

}
