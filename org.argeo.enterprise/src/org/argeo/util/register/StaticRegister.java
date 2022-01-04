package org.argeo.util.register;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** A minimal component register. */
public class StaticRegister {
	private final static AtomicBoolean started = new AtomicBoolean(false);
	private final static IdentityHashMap<Object, Component<?>> components = new IdentityHashMap<>();

	static synchronized void registerComponent(Component<?> component) {
		if (started.get()) // TODO make it really dynamic
			throw new IllegalStateException("Already activated");
		if (components.containsKey(component.getInstance()))
			throw new IllegalArgumentException("Already registered as component");
		components.put(component.getInstance(), component);
	}

	static synchronized Component<?> get(Object instance) {
		if (!components.containsKey(instance))
			throw new IllegalArgumentException("Not registered as component");
		return components.get(instance);
	}

	synchronized static void activate() {
		if (started.get())
			throw new IllegalStateException("Already activated");
		Set<CompletableFuture<?>> constraints = new HashSet<>();
		for (Component<?> component : components.values()) {
			component.startActivating();
			constraints.add(component.getActivated());
		}

		// wait
		try {
			CompletableFuture.allOf(constraints.toArray(new CompletableFuture[0])).thenRun(() -> started.set(true))
					.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	synchronized static void deactivate() {
		if (!started.get())
			throw new IllegalStateException("Not activated");
		Set<CompletableFuture<?>> constraints = new HashSet<>();
		for (Component<?> component : components.values()) {
			component.startDeactivating();
			constraints.add(component.getDeactivated());
		}

		// wait
		try {
			CompletableFuture.allOf(constraints.toArray(new CompletableFuture[0])).thenRun(() -> started.set(false))
					.get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
