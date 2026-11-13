package com.hubspot.singularity.config.server;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A size written the way the configuration file writes one: a number and a unit ("256 bytes", "8KiB").
 */
public final class DataSize {
  private static final Pattern SIZE = Pattern.compile(
    "^\\s*(\\d+)\\s*([a-zA-Z]*)\\s*$"
  );

  private DataSize() {}

  public static long parse(String value) {
    if (value == null) {
      throw new IllegalArgumentException("size is null");
    }
    Matcher matcher = SIZE.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid size: " + value);
    }
    long count = Long.parseLong(matcher.group(1));
    String unit = matcher.group(2).toLowerCase(Locale.ENGLISH);
    switch (unit) {
      case "":
      case "b":
      case "byte":
      case "bytes":
        return count;
      case "kb":
      case "kib":
      case "kilobyte":
      case "kilobytes":
        return count * 1024L;
      case "mb":
      case "mib":
      case "megabyte":
      case "megabytes":
        return count * 1024L * 1024L;
      case "gb":
      case "gib":
      case "gigabyte":
      case "gigabytes":
        return count * 1024L * 1024L * 1024L;
      default:
        throw new IllegalArgumentException("Invalid size: " + value);
    }
  }
}
