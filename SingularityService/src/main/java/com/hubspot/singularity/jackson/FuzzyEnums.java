package com.hubspot.singularity.jackson;

/**
 * Reading an enumerated value from text the way the service has always read one: whitespace anywhere in the
 * text is dropped, a dash or a period stands for an underscore, and the comparison against the constants
 * ignores case. A constant whose own text spells the value is accepted as well, compared against the value
 * as it was given.
 */
public final class FuzzyEnums {

  private FuzzyEnums() {}

  public static Enum<?> fromStringFuzzy(String value, Enum<?>[] constants) {
    if (value == null) {
      return null;
    }
    final String text = removeWhitespace(value).replace('-', '_').replace('.', '_');
    for (Enum<?> constant : constants) {
      if (constant.name().equalsIgnoreCase(text)) {
        return constant;
      }
    }
    for (Enum<?> constant : constants) {
      if (constant.toString().equalsIgnoreCase(value)) {
        return constant;
      }
    }
    return null;
  }

  /** The text without any of the characters that count as whitespace when a value is read. */
  private static String removeWhitespace(String value) {
    StringBuilder kept = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (!isWhitespace(c)) {
        kept.append(c);
      }
    }
    return kept.toString();
  }

  /**
   * Whether a character counts as whitespace when an enumerated value is read. The set is the one the
   * service's character matching has always used, which is wider than the one the language's own method
   * answers for: every character the Unicode tables call a space separator, the line and paragraph
   * separators, the next-line character, and the ASCII control characters that space text out.
   */
  private static boolean isWhitespace(char c) {
    if (c == 0x09 || c == 0x0a || c == 0x0b || c == 0x0c || c == 0x0d) {
      return true;
    }
    if (c == 0x1c || c == 0x1d || c == 0x1e || c == 0x1f) {
      return true;
    }
    if (c == 0x20 || c == 0x85 || c == 0xa0 || c == 0x1680) {
      return true;
    }
    if (c >= 0x2000 && c <= 0x200a) {
      return true;
    }
    if (c == 0x2028 || c == 0x2029 || c == 0x202f || c == 0x205f || c == 0x3000) {
      return true;
    }
    return false;
  }

  /** The constants of an enumerated type, written the way a refusal lists them. */
  public static String listConstants(Enum<?>[] constants) {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < constants.length; i++) {
      if (i > 0) {
        text.append(", ");
      }
      text.append(constants[i].name());
    }
    return text.toString();
  }
}
