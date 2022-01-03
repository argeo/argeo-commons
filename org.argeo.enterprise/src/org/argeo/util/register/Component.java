package org.argeo.util.register;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Component {
	private final static AtomicBoolean started = new AtomicBoolean(false);
	private final static IdentityHashMap<Object, Component> components = new IdentityHashMap<>();

	private static synchronized void registerComponent(Component component) {
		if (started.get()) // TODO make it rellay dynamic
			throw new IllegalStateException("Already activated");
		if (components.containsKey(component.instance))
			throw new IllegalArgumentException("Already registered as component");
		components.put(component.instance, component);
	}

	static synchronized Component get(Object instance) {
		if (!components.containsKey(instance))
			throw new IllegalArgumentException("Not registered as component");
		return components.get(instance);
	}

	public synchronized static void activate() {
		if (started.get())
			throw new IllegalStateException("Already activated");
		for (Component component : components.values()) {
			component.activationStarted.complete(null);
		}
		started.set(true);
	}

	public synchronized static void deactivate() {
		if (!started.get())
			throw new IllegalStateException("Not activated");
		for (Component component : components.values()) {
			component.deactivationStarted.complete(null);
		}
		started.set(false);
	}

	private final Object instance;

	private Runnable init;
	private Runnable close;

	private final Map<Class<?>, PublishedType<?>> types;
	private final Set<Dependency<?>> dependencies;

	private CompletableFuture<Void> activationStarted = new CompletableFuture<Void>();
	private CompletableFuture<Void> activated = new CompletableFuture<Void>();

	private CompletableFuture<Void> deactivationStarted = new CompletableFuture<Void>();
	private CompletableFuture<Void> deactivated = new CompletableFuture<Void>();

	private Set<Dependency<?>> dependants = new HashSet<>();

	Component(Object instance, Runnable init, Runnable close, Set<Dependency<?>> dependencies, Set<Class<?>> classes) {
		assert instance != null;
		assert init != null;
		assert close != null;
		assert dependencies != null;
		assert classes != null;

		this.instance = instance;
		this.init = init;
		this.close = close;

		// types
		Map<Class<?>, PublishedType<?>> types = new HashMap<>(classes.size());
		for (Class<?> clss : classes) {
			if (!clss.isAssignableFrom(instance.getClass()))
				throw new IllegalArgumentException(
						"Type " + clss.getName() + " is not compatible with " + instance.getClass().getName());
			types.put(clss, new PublishedType<>(clss));
		}
		this.types = Collections.unmodifiableMap(types);

		// dependencies
		this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
		for (Dependency<?> dependency : this.dependencies) {
			dependency.setDependantComponent(this);
		}

		// future activation
		activated = activationStarted //
				.thenCompose(this::dependenciesActivated) //
				.thenRun(this.init);

		// future deactivation
		deactivated = deactivationStarted //
				.thenCompose(this::dependantsDeactivated) //
				.thenRun(this.close);

		registerComponent(this);
	}

	CompletableFuture<Void> dependenciesActivated(Void v) {
		Set<CompletableFuture<?>> constraints = new HashSet<>(this.dependencies.size());
		for (Dependency<?> dependency : this.dependencies) {
			CompletableFuture<Void> dependencyActivated = dependency.getPublisher().activated //
					.thenCompose(dependency::set);
			constraints.add(dependencyActivated);
		}
		return CompletableFuture.allOf(constraints.toArray(new CompletableFuture[constraints.size()]));
	}

	CompletableFuture<Void> dependantsDeactivated(Void v) {
		Set<CompletableFuture<?>> constraints = new HashSet<>(this.dependants.size());
		for (Dependency<?> dependant : this.dependants) {
			CompletableFuture<Void> dependantDeactivated = dependant.getDependantComponent().deactivated //
					.thenCompose(dependant::unset);
			constraints.add(dependantDeactivated);
		}
		CompletableFuture<Void> dependantsDeactivated = CompletableFuture
				.allOf(constraints.toArray(new CompletableFuture[constraints.size()]));
		return dependantsDeactivated;

	}

	void addDependant(Dependency<?> dependant) {
		dependants.add(dependant);
	}

	public <T> PublishedType<T> getType(Class<T> clss) {
		if (!types.containsKey(clss))
			throw new IllegalArgumentException(clss.getName() + " is not a type published by this component");
		return (PublishedType<T>) types.get(clss);
	}

	public class PublishedType<T> {
		private Class<T> clss;

		private CompletableFuture<T> value;

		public PublishedType(Class<T> clss) {
			this.clss = clss;

			value = CompletableFuture.completedFuture((T) Component.this.instance);
		}

		Component getPublisher() {
			return Component.this;
		}

		Class<T> getType() {
			return clss;
		}
	}

	public static class Builder<I> {
		private final I instance;

		private Runnable init;
		private Runnable close;

		private Set<Dependency<?>> dependencies = new HashSet<>();
		private Set<Class<?>> types = new HashSet<>();

		public Builder(I instance) {
			this.instance = instance;
		}

		public Component build() {
			if (types.isEmpty()) {
				types.add(instance.getClass());
			}

			if (init == null)
				init = () -> {
				};
			if (close == null)
				close = () -> {
				};

			Component component = new Component(instance, init, close, dependencies, types);
			for (Dependency<?> dependency : dependencies) {
				dependency.type.getPublisher().addDependant(dependency);
			}
			return component;
		}

		public Builder<I> addType(Class<?>... classes) {
			types.addAll(Arrays.asList(classes));
			return this;
		}

		public Builder<I> addInit(Runnable init) {
			if (this.init != null)
				throw new IllegalArgumentException("init method is already set");
			this.init = init;
			return this;
		}

		public Builder<I> addClose(Runnable close) {
			if (this.close != null)
				throw new IllegalArgumentException("close method is already set");
			this.close = close;
			return this;
		}

		public <D> Builder<I> addDependency(PublishedType<D> type, Predicate<?> filter, Consumer<D> set,
				Consumer<D> unset) {
			dependencies.add(new Dependency<D>(type, filter, set, unset));
			return this;
		}

		public I get() {
			return instance;
		}

	}

	static class Dependency<D> {
		private PublishedType<D> type;
		private Predicate<?> filter;
		private Consumer<D> set;
		private Consumer<D> unset;

		// live
		Component dependantComponent;
		CompletableFuture<Void> setStage;
		CompletableFuture<Void> unsetStage;

		public Dependency(PublishedType<D> types, Predicate<?> filter, Consumer<D> set, Consumer<D> unset) {
			super();
			this.type = types;
			this.filter = filter;
			this.set = set;
			this.unset = unset != null ? unset : (v) -> set.accept(null);
		}

		// live
		void setDependantComponent(Component component) {
			this.dependantComponent = component;
		}

		Component getPublisher() {
			return type.getPublisher();
		}

		Component getDependantComponent() {
			return dependantComponent;
		}

		CompletableFuture<Void> set(Void v) {
			return type.value.thenAccept(set);
		}

		CompletableFuture<Void> unset(Void v) {
			return type.value.thenAccept(unset);
		}

	}
}

