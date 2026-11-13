package com.hubspot.singularity.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * The configuration file as it lies on the file system. A path that names nothing is refused with the
 * message the service has always refused it with.
 */
public class FileConfigurationSourceProvider implements ConfigurationSourceProvider {

  @Override
  public InputStream open(String path) throws IOException {
    final File file = new File(path);
    if (!file.exists()) {
      throw new FileNotFoundException("File " + file + " not found");
    }
    return new FileInputStream(file);
  }
}
