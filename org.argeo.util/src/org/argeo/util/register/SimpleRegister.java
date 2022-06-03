package org.argeo.util.register;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/** A minimal component register. */
public class SimpleRegister implements ComponentRegister {
	private final AtomicBoolean started = new AtomicBoolean(false);
	private final IdentityHashMap<Object, Component<?>> components = new IdentityHashMap<>();
	private final AtomicLong nextServiceId = new AtomicLong(0l);

	@Override
	public long register(Component<?> component) {
		return registerComponent(component);
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public synchronized <T> SortedSet<Component<? extends T>> find(Class<T> clss,
			Predicate<Map<String, Object>> filter) {
		SortedSet<Component<? extends T>> result = new TreeSet<>();
		instances: for (Object instance : components.keySet()) {
			if (!clss.isAssignableFrom(instance.getClass()))
				continue instances;
			Component<? extends T> component = (Component<? extends T>) components.get(instance);

			if (component.isPublishedType(clss)) {
				if (filter != null) {
					filter.test(component.getProperties());
				}
				result.add(component);
			}
		}
		if (result.isEmpty())
			return null;
		return result;

	}

	synchronized long registerComponent(Component<?> component) {
		if (started.get()) // TODO make it really dynamic
			throw new IllegalStateException("Already activated");
		if (components.containsKey(component.get()))
			throw new IllegalArgumentException("Already registered as component");
		components.put(component.get(), component);
		return nextServiceId.incrementAndGet();
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
