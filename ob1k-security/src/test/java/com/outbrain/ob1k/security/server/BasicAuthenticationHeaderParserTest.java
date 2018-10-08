package com.outbrain.ob1k.security.server;

import com.outbrain.ob1k.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;

public class BasicAuthenticationHeaderParserTest {

  private static final String USER = "user";
  private static final String PASSWORD = "password";
  private static final String CREDENTIALS = USER + ":" + PASSWORD;
  private static final String ENCODED_CREDENTIALS = Base64.getEncoder().encodeToString(CREDENTIALS.getBytes());

  private BasicAuthenticationHeaderParser parser;
  private Request request;

  @Before
  public void setup() {
    parser = new BasicAuthenticationHeaderParser();
    request = mock(Request.class);
  }

  @After
  public void tearDown() {
    parser = null;
    request = null;
  }

  @Test
  public void testNoHeader() {
    when(request.getHeader("Authorization")).thenReturn(null);
    final Credentials<UserPasswordToken> extractedCredentials = parser.extractCredentials(request);
    assertNull(extractedCredentials);
  }

  @Test
  public void testHeaderWithoutBasicPrefix() {
    when(request.getHeader("Authorization")).thenReturn("user:password");
    final Credentials<UserPasswordToken> extractedCredentials = parser.extractCredentials(request);
    assertNull(extractedCredentials);
  }

  @Test
  public void testHeaderWithInvalidEncodingCredentials() {
    when(request.getHeader("Authorization")).thenReturn("Basic something");
    final Credentials<UserPasswordToken> extractedCredentials = parser.extractCredentials(request);
    assertNull(extractedCredentials);
  }

  @Test
  public void testInvalidHeader() {
    when(request.getHeader("Authorization")).thenReturn("Basic c29tZXRoaW5n");
    final Credentials<UserPasswordToken> extractedCredentials = parser.extractCredentials(request);
    assertNull(extractedCredentials);
  }

  @Test
  public void testValidHeader() {
    when(request.getHeader("Authorization")).thenReturn("Basic " + ENCODED_CREDENTIALS);
    final Credentials<UserPasswordToken> extractedCredentials = parser.extractCredentials(request);
    assertEquals(USER, extractedCredentials.get().getUsername());
    assertEquals(PASSWORD, new String(extractedCredentials.get().getPassword()));
  }
}
