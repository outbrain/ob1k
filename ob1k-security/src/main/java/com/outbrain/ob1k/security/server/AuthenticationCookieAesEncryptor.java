package com.outbrain.ob1k.security.server;

import com.ning.http.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * A cookie encryptor that use 128bit AES encryption
 */
public class AuthenticationCookieAesEncryptor implements AuthenticationCookieEncryptor {

  public static final String AES_ALGORITHM = "AES";
  public static final String UTF8 = "UTF-8";

  private final ThreadLocal<Cipher> decryptingCipher;
  private final ThreadLocal<Cipher> encryptingCipher;

  /**
   * @param key a 128bit key
   */
  public AuthenticationCookieAesEncryptor(final byte[] key) {

    final SecretKey secretKey = new SecretKeySpec(key, AES_ALGORITHM);

    decryptingCipher = createCipher(secretKey, Cipher.DECRYPT_MODE);
    encryptingCipher = createCipher(secretKey, Cipher.ENCRYPT_MODE);
  }

  private ThreadLocal<Cipher> createCipher(final SecretKey secretKey, final int decryptMode) {
    return new ThreadLocal<Cipher>() {
      @Override
      protected Cipher initialValue() {
        final Cipher cipher;
        try {
          cipher = Cipher.getInstance(AES_ALGORITHM);
          cipher.init(decryptMode, secretKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
          throw new RuntimeException("Error initializing", e);
        } catch (InvalidKeyException e) {
          throw new RuntimeException("Invalid key", e);
        }
        return cipher;
      }
    };
  }

  @Override
  public AuthenticationCookie decrypt(final String encryptedCookie) {
    final byte[] encryptedBytes = Base64.decode(encryptedCookie);
    try {
      final byte[] decryptedBytes = decryptingCipher.get().doFinal(encryptedBytes);
      return AuthenticationCookie.fromDelimitedString(new String(decryptedBytes, UTF8));
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error decrypting cookie " + encryptedCookie, e);
    }
  }

  @Override
  public String encrypt(final AuthenticationCookie authenticationCookie) {
    try {
      final byte[] cookieBytes = authenticationCookie.toDelimitedString().getBytes(UTF8);
      final byte[] encryptedBytes = encryptingCipher.get().doFinal(cookieBytes);
      return Base64.encode(encryptedBytes);
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error encrypting cookie " + authenticationCookie, e);
    }
  }
}
