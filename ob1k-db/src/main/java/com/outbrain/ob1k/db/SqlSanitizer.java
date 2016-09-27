package com.outbrain.ob1k.db;

/**
 * Provides basic utility to filter inputs when preparing SQL statements
 *
 * @author marenzon
 */
public class SqlSanitizer {

  private SqlSanitizer() {
  }

  public static String escapeInput(final String input) {
    if (input == null) {
      return null;
    }

    final int inputLength = input.length();
    final StringBuilder sanitizedInput = new StringBuilder((int) (inputLength * 1.1));

    // This code is taken from official MySQL j/connector driver
    for (int i = 0; i < inputLength; ++i) {
      final char c = input.charAt(i);

      switch (c) {
        case '\0': /* Must be escaped for 'mysql' */
          sanitizedInput.append('\\');
          sanitizedInput.append('0');

          break;

        case '\n': /* Must be escaped for logs */
          sanitizedInput.append('\\');
          sanitizedInput.append('n');

          break;

        case '\r':
          sanitizedInput.append('\\');
          sanitizedInput.append('r');

          break;

        case '\\':
          sanitizedInput.append('\\');
          sanitizedInput.append('\\');

          break;

        case '\'':
          sanitizedInput.append('\\');
          sanitizedInput.append('\'');

          break;

        case '"': /* Better safe than sorry */
          sanitizedInput.append('\\');
          sanitizedInput.append('"');

          break;

        case '\032': /* This gives problems on Win32 */
          sanitizedInput.append('\\');
          sanitizedInput.append('Z');

          break;

        default:
          sanitizedInput.append(c);
      }
    }

    return sanitizedInput.toString();
  }
}
