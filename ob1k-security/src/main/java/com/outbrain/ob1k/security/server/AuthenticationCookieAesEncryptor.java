package com.outbrain.ob1k.security.server;

import com.ning.http.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * A cookie encryptor that use 128bit AES encryption
 */
public class AuthenticationCookieAesEncryptor implements AuthenticationCookieEncryptor {

  private static final String AES_ALGORITHM = "AES";
  private static final String UTF8 = "UTF-8";

  private static final ThreadLocal<Map<Key, Cipher>> decryptingCipher = new ThreadLocal<Map<Key, Cipher>>() {
    @Override
    protected Map<Key, Cipher> initialValue() {
      return new HashMap<>();
    }
  };

  private static final ThreadLocal<Map<Key, Cipher>> encryptingCipher = new ThreadLocal<Map<Key, Cipher>>() {
    @Override
    protected Map<Key, Cipher> initialValue() {
      return new HashMap<>();
    }
  };

  private final Key cipherKey;

  /**
   * @param key a 128bit key
   */
  public AuthenticationCookieAesEncryptor(final byte[] key) {
    this.cipherKey = new SecretKeySpec(key, AES_ALGORITHM);
  }

  private Cipher getDecryptingCipher() {
    final Map<Key, Cipher> cipherMap = decryptingCipher.get();
    Cipher cipher = cipherMap.get(cipherKey);
    if (cipher == null) {
      cipher = createCipher(cipherKey, Cipher.DECRYPT_MODE);
      cipherMap.put(cipherKey, cipher);
    }

    return cipher;
  }

  private Cipher getEncryptingCipher() {
    final Map<Key, Cipher> cipherMap = encryptingCipher.get();
    Cipher cipher = cipherMap.get(cipherKey);
    if (cipher == null) {
      cipher = createCipher(cipherKey, Cipher.ENCRYPT_MODE);
      cipherMap.put(cipherKey, cipher);
    }

    return cipher;
  }

  private static Cipher createCipher(final Key key, final int mode) {
    try {
      final Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(mode, key);
      return cipher;
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new RuntimeException("Error initializing", e);
    } catch (final InvalidKeyException e) {
      throw new RuntimeException("Invalid key", e);
    }
  }

  @Override
  public AuthenticationCookie decrypt(final String encryptedCookie) {
    final byte[] encryptedBytes = Base64.decode(encryptedCookie);
    try {
      final byte[] decryptedBytes = getDecryptingCipher().doFinal(encryptedBytes);
      return AuthenticationCookie.fromDelimitedString(new String(decryptedBytes, UTF8));
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error decrypting cookie " + encryptedCookie, e);
    }
  }

  @Override
  public String encrypt(final AuthenticationCookie authenticationCookie) {
    try {
      final byte[] cookieBytes = authenticationCookie.toDelimitedString().getBytes(UTF8);
      final byte[] encryptedBytes = getEncryptingCipher().doFinal(cookieBytes);
      return Base64.encode(encryptedBytes);
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error encrypting cookie " + authenticationCookie, e);
    }
  }

}
