package com.hubspot.singularity.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Where the service is running from, which is the name the command line calls itself by and where the
 * version it reports comes from.
 */
public class JarLocation {
  private final Class<?> klass;

  public JarLocation(Class<?> klass) {
    this.klass = klass;
  }

  /** The version the jar's manifest carries, or nothing when the service runs from class files. */
  public Optional<String> getVersion() {
    try {
      final Enumeration<URL> resources = klass
        .getClassLoader()
        .getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        try (java.io.InputStream input = resources.nextElement().openStream()) {
          final Manifest manifest = new Manifest(input);
          final Attributes attributes = manifest.getMainAttributes();
          if (
            klass.getName().equals(attributes.getValue("Start-Class")) ||
            klass.getName().equals(attributes.getValue("Main-Class"))
          ) {
            return Optional.ofNullable(attributes.getValue("Implementation-Version"));
          }
        }
      }
    } catch (IOException ignored) {
      // a jar without a readable manifest reports no version
    }
    return Optional.empty();
  }

  /**
   * The name the command line calls itself by, which is the name of the jar the service runs from. The jar
   * the service is packaged as holds its classes inside itself, so the name is taken from what the service
   * was started with rather than from where the class was loaded from.
   */
  @Override
  public String toString() {
    final String started = System.getProperty("java.class.path", "");
    if (!started.isEmpty() && !started.contains(java.io.File.pathSeparator)) {
      final String name = Paths.get(started).getFileName().toString();
      if (name.endsWith(".jar")) {
        return name;
      }
    }
    try {
      final URL location = klass.getProtectionDomain().getCodeSource().getLocation();
      final String name = Paths.get(location.toURI()).getFileName().toString();
      return name.endsWith(".jar") ? name : "project.jar";
    } catch (URISyntaxException | RuntimeException e) {
      return "project.jar";
    }
  }
}
