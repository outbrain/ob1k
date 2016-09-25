package com.outbrain.ob1k.db;

import java.util.regex.Pattern;

/**
 * Provides basic utility to filter inputs when preparing SQL statements
 *
 * @author marenzon
 */
public class SqlSanitizer {

  private static final Pattern NULLBYTE_PATTERN = Pattern.compile("\\\\x00");
  private static final Pattern SUB_PATTERN = Pattern.compile("\\\\x1a");
  private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile("'");
  private static final Pattern DOUBLE_QUOTE_PATTERN = Pattern.compile("\\\"");
  private static final Pattern BACKSLASH_PATTERN = Pattern.compile("\\\\");

  private SqlSanitizer() {
  }

  public static String escapeInput(final String input) {
    if (input == null) {
      return null;
    }

    String fixedConstant = input;
    fixedConstant = NULLBYTE_PATTERN.matcher(fixedConstant).replaceAll("");
    fixedConstant = SUB_PATTERN.matcher(fixedConstant).replaceAll("");
    fixedConstant = SINGLE_QUOTE_PATTERN.matcher(fixedConstant).replaceAll("''");
    fixedConstant = DOUBLE_QUOTE_PATTERN.matcher(fixedConstant).replaceAll("\\\\\"");
    fixedConstant = BACKSLASH_PATTERN.matcher(fixedConstant).replaceAll("\\\\\\\\");

    return fixedConstant;
  }
}
