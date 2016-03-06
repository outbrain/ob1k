package com.outbrain.ob1k.server;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.junit.Test;

import com.outbrain.ob1k.concurrent.ComposableFuture;

public class MethodParamNamesExtractorTest {

  @Test
  public void testExtractInstanceMethod() throws Exception {
    final Method testedMethod = getClass().getMethod("_mockMethod1", int.class, long.class, int.class, int.class);
    final Collection<Method> methods = Collections.singleton(testedMethod);
    final Map<Method, List<String>> methodMap = MethodParamNamesExtractor.extract(getClass(), methods);

    final List<String> paramNames = methodMap.get(testedMethod);
    assertEquals("method param count", 4, paramNames.size());
    final List<String> expectedNames = Arrays.asList("p1", "p2", "p3", "p4");
    int index = 0;
    for (final String paramName : paramNames) {
      assertNotNull(paramName, paramName);
      assertEquals(paramName, expectedNames.get(index++));
    }
  }

  @Test
  public void testExtractStaticMethod() throws Exception {
    final Method testedMethod = getClass().getMethod("_mockMethod2", double.class, float.class, Object.class);
    final Collection<Method> methods = Collections.singleton(testedMethod);
    final Map<Method, List<String>> methodMap = MethodParamNamesExtractor.extract(getClass(), methods);

    final List<String> paramNames = methodMap.get(testedMethod);
    assertEquals("method param count", 3, paramNames.size());
    final List<String> expectedNames = Arrays.asList("d1", "f1", "o1");
    int index = 0;
    for (final String paramName : paramNames) {
      assertNotNull(paramName, paramName);
      assertEquals(paramName, expectedNames.get(index++));
    }
  }

  // This method is used for testing...
  public ComposableFuture<Object> _mockMethod1(final int p1, final long p2, final int p3, final int p4) {
    final int p5 = 1;
    final int p6 = 2;

    try {
      final int p7 = 2;
      final int p8  = 3;
    } catch (final Exception e) {
      final int p9 = 3;
    }
    final int p10 = 3;
    return null;
  }

  // This method is used for testing...
  public static void _mockMethod2(final double d1, final float f1, final Object o1) {
    final int i1 = 1;
    try {
      final int i2 = 2;
    } catch (final Exception ex) {
      final int i3 = 3;
    }

    final int i4 = 4;
  }
}
