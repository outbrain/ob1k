package com.outbrain.ob1k.security.server;

public interface AuthenticationCookieEncryptor {

  AuthenticationCookie decrypt(final String encryptedCookie);

  String encrypt(final AuthenticationCookie authenticationCookie);

}
