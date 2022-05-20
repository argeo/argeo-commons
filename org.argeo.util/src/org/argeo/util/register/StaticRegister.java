package org.argeo.util.register;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/** A minimal component register. */
public class StaticRegister implements ComponentRegister {
	private final static StaticRegister instance = new StaticRegister();

	public static ComponentRegister getInstance() {
		return instance;
	}

	private final AtomicBoolean started = new AtomicBoolean(false);
	private final IdentityHashMap<Object, Component<?>> components = new IdentityHashMap<>();

	@Override
	public void accept(Component<?> component) {
		registerComponent(component);
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public synchronized <T> Component<? extends T> find(Class<T> clss, Predicate<Map<String, Object>> filter) {
		Set<Component<? extends T>> result = new HashSet<>();
		instances: for (Object instance : components.keySet()) {
			if (!clss.isAssignableFrom(instance.getClass()))
				continue instances;
			Component<? extends T> component = (Component<? extends T>) components.get(instance);

			// TODO filter
			if (component.isPublishedType(clss))
				result.add(component);
		}
		if (result.isEmpty())
			return null;
		return result.iterator().next();

	}

	synchronized void registerComponent(Component<?> component) {
		if (started.get()) // TODO make it really dynamic
			throw new IllegalStateException("Already activated");
		if (components.containsKey(component.getInstance()))
			throw new IllegalArgumentException("Already registered as component");
		components.put(component.getInstance(), component);
	}

	@Override
	public synchronized Component<?> get(Object instance) {
		if (!components.containsKey(instance))
			throw new IllegalArgumentException("Not registered as component");
		return components.get(instance);
	}

	@Override
	public synchronized void activate() {
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
		} catch (InterruptedException e) {
			throw new RuntimeException("Register activation has been interrupted", e);
		} catch (ExecutionException e) {
			if (RuntimeException.class.isAssignableFrom(e.getCause().getClass())) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new IllegalStateException("Cannot activate register", e.getCause());
			}
		}
	}

	@Override
	public synchronized void deactivate() {
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
		} catch (InterruptedException e) {
			throw new RuntimeException("Register deactivation has been interrupted", e);
		} catch (ExecutionException e) {
			if (RuntimeException.class.isAssignableFrom(e.getCause().getClass())) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new IllegalStateException("Cannot deactivate register", e.getCause());
			}
		}
	}

	@Override
	public synchronized boolean isActive() {
		return started.get();
	}

	@Override
	public synchronized void clear() {
		components.clear();
	}

}
