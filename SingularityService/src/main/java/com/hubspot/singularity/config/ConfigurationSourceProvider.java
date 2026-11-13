package com.hubspot.singularity.config;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where the text of a configuration file comes from. The service reads one from the file system, and reads
 * one merged with a file of defaults when it is told to.
 */
public interface ConfigurationSourceProvider {
  InputStream open(String path) throws IOException;
}
