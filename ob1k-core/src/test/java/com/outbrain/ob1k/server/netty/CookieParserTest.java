package com.outbrain.ob1k.server.netty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author guymarom
 */
public class CookieParserTest {

  private CookieParser cookieParser;

  @Before
  public void setUp() throws Exception {
    cookieParser = new CookieParser();
  }

  @After
  public void tearDown() throws Exception {
    cookieParser = null;
  }

  @Test
  public void testNullString() {
    assertEquals(0, cookieParser.parse(null).size());
  }

  @Test
  public void testEmptyString() {
    assertEquals(0, cookieParser.parse("").size());
  }

  @Test
  public void testInvalidString() {
    assertEquals(0, cookieParser.parse("=sdf;sfsdfsdf").size());
  }

  @Test
  public void testOneCookie() {
    final String name = "name";
    final String value = "value";
    final Map<String, String> result = cookieParser.parse(name + "=" + value);
    assertEquals(1, result.size());
    assertEquals(value, result.get(name));
  }

  @Test
  public void testMultipleCookies() {
    final String name1 = "name1";
    final String name2 = "name2";
    final String value1 = "value1";
    final String value2 = "value2";
    final Map<String, String> result = cookieParser.parse(name1 + "=" + value1 + ";" + name2 + "=" + value2);
    assertEquals(2, result.size());
    assertEquals(value1, result.get(name1));
    assertEquals(value2, result.get(name2));
  }

  @Test
  public void testValidAndInvalid() {
    final String name = "name";
    final String value = "value";
    final String invalid = "value1";
    final Map<String, String> result = cookieParser.parse(name + "=" + value + ";" + invalid);
    assertEquals(1, result.size());
    assertEquals(value, result.get(name));
  }
}