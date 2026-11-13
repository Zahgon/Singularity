package com.hubspot.singularity;

import com.google.inject.Provider;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

/**
 * How the service stops the connectors it answers on, once they are up. Nothing is there to stop before
 * the service has finished starting, which is what an absent value means.
 */
public class ServerProvider implements Provider<Optional<Runnable>> {
  private final AtomicReference<Runnable> stop = new AtomicReference<>();

  @Inject
  public ServerProvider() {}

  @Override
  public Optional<Runnable> get() {
    return Optional.ofNullable(stop.get());
  }

  /** Called once the service answers requests, with what stops it answering them. */
  public void serverStarted(Runnable stop) {
    this.stop.set(stop);
  }
}
