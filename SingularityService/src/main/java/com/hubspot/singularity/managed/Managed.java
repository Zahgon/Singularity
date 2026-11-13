package com.hubspot.singularity.managed;

/**
 * Something the service starts when it comes up and stops when it goes down. The service's own objects
 * implement this and the application's lifecycle drives them, in the order they are registered on the way
 * up and in the reverse order on the way down.
 */
public interface Managed {
  void start() throws Exception;

  void stop() throws Exception;
}
