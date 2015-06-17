package com.outbrain.ob1k.security.server;

import com.ning.http.util.Base64;
import org.apache.commons.lang3.SerializationUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * A cookie encryptor that use 128bit AES encryption
 */
public class AuthenticationCookieAesEncryptor implements AuthenticationCookieEncryptor {

  public static final String AES_ALGORITHM = "AES";

  private static ThreadLocal<Cipher> decryptingCipher;
  private static ThreadLocal<Cipher> encryptingCipher;

  /**
   * @param key a 128bit key
   */
  public AuthenticationCookieAesEncryptor(final byte[] key) throws NoSuchPaddingException,
    NoSuchAlgorithmException,
    InvalidKeyException {

    final SecretKey secretKey = new SecretKeySpec(key, AES_ALGORITHM);

    final Cipher decCypher = Cipher.getInstance(AES_ALGORITHM);
    decCypher.init(Cipher.DECRYPT_MODE, secretKey);
    decryptingCipher = new ThreadLocal<>();
    decryptingCipher.set(decCypher);

    final Cipher encCypher = Cipher.getInstance(AES_ALGORITHM);
    encCypher.init(Cipher.ENCRYPT_MODE, secretKey);
    encryptingCipher = new ThreadLocal<>();
    encryptingCipher.set(encCypher);
  }

  @Override
  public AuthenticationCookie decrypt(final String encryptedCookie) {
    final byte[] encryptedBytes = Base64.decode(encryptedCookie);
    try {
      final byte[] decryptedBytes = decryptingCipher.get().doFinal(encryptedBytes);
      return (AuthenticationCookie) SerializationUtils.deserialize(decryptedBytes);
    } catch (IllegalBlockSizeException| BadPaddingException e) {
      throw new RuntimeException("Error decrypting cookie " + encryptedCookie, e);
    }
  }

  @Override
  public String encrypt(final AuthenticationCookie authenticationCookie) {
    final byte[] cookieBytes = SerializationUtils.serialize(authenticationCookie);
    try {
      final byte[] encryptedBytes = encryptingCipher.get().doFinal(cookieBytes);
      return Base64.encode(encryptedBytes);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new RuntimeException("Error encrypting cookie " + authenticationCookie, e);
    }
  }
}
