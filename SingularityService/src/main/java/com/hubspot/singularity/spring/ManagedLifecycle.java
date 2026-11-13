package com.hubspot.singularity.spring;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.hubspot.singularity.managed.Managed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Starts everything the object graph holds that the service manages, before the connector takes its first
 * request, and stops them in the reverse order on the way down. The bindings decide what is managed, which
 * is how the service has always decided it: a binding of a managed type is started.
 */
public class ManagedLifecycle implements SmartLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(ManagedLifecycle.class);

  private final Injector injector;
  private final List<Managed> managed = new ArrayList<>();
  private volatile boolean running;

  public ManagedLifecycle(Injector injector) {
    this.injector = injector;
  }

  @Override
  public void start() {
    for (Key<?> key : injector.getAllBindings().keySet()) {
      if (Managed.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
        final Binding<?> binding = injector.getExistingBinding(key);
        if (binding != null) {
          final Object instance = binding.getProvider().get();
          if (instance instanceof Managed && !managed.contains(instance)) {
            managed.add((Managed) instance);
          }
        }
      }
    }
    for (Managed object : managed) {
      try {
        object.start();
      } catch (Exception e) {
        throw new IllegalStateException(
          "Could not start " + object.getClass().getSimpleName(),
          e
        );
      }
    }
    running = true;
  }

  @Override
  public void stop() {
    running = false;
    final List<Managed> reversed = new ArrayList<>(managed);
    Collections.reverse(reversed);
    for (Managed object : reversed) {
      try {
        object.stop();
      } catch (Exception e) {
        LOG.warn("Could not stop {} ({})", object.getClass().getSimpleName(), e.getMessage());
      }
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
