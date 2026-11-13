package com.hubspot.singularity.managed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * This class runs shutdown hooks before the rest of the service is shut down. Actions will be run serially -
 * any exceptions will be swallowed until after all have run.
 *
 * This class does not need to be created after the server.
 */
@Singleton
public class SingularityPreJettyLifecycle implements Managed {
  private final List<Runnable> hooks;
  private final AtomicBoolean hasRun;

  @Inject
  public SingularityPreJettyLifecycle() {
    this.hooks = new CopyOnWriteArrayList<>();
    this.hasRun = new AtomicBoolean();
  }

  @Override
  public void start() {}

  /** Runs every hook, and only then answers for the first one that could not be run. */
  @Override
  public synchronized void stop() throws Exception {
    if (hasRun.get()) {
      return;
    }

    Exception failure = null;
    for (Runnable hook : hooks) {
      try {
        hook.run();
      } catch (Exception e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    hasRun.set(true);
    if (failure != null) {
      throw failure;
    }
  }

  public boolean registerShutdownHook(Runnable runnable) {
    return hooks.add(runnable);
  }
}
