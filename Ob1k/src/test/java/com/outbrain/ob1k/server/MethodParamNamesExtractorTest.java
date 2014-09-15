package com.outbrain.ob1k.server;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.outbrain.ob1k.concurrent.ComposableFuture;

public class MethodParamNamesExtractorTest {

  @Test
  public void testExtract() throws Exception {
    final Method testedMethod = getClass().getMethod("_mockMethod1", int.class, long.class, int.class, int.class, int.class, int.class);
    final Collection<Method> methods = Collections.singleton(testedMethod);
    final Map<Method, List<String>> methodMap = MethodParamNamesExtractor.extract(getClass(), methods);

    final List<String> paramNames = methodMap.get(testedMethod);
    assertEquals("method param count", 6, paramNames.size());
    System.out.println(paramNames);
    for (String paramName : paramNames) {
      assertNotNull(paramName, paramName);
    }
  }

  // This method is used for testing...
  public ComposableFuture<Object> _mockMethod1(final int p1, final long p2, final int p3, final int p4, final int p5, final int p6) {
    return null;
  }
}
