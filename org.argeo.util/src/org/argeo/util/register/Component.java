package org.argeo.util.register;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A wrapper for an object, whose dependencies and life cycle can be managed.
 */
public class Component<I> {

	private final I instance;

	private final Runnable init;
	private final Runnable close;

	private final Map<Class<? super I>, PublishedType<? super I>> types;
	private final Set<Dependency<?>> dependencies;

	private CompletableFuture<Void> activationStarted = null;
	private CompletableFuture<Void> activated = null;

	private CompletableFuture<Void> deactivationStarted = null;
	private CompletableFuture<Void> deactivated = null;

	private Set<Dependency<?>> dependants = new HashSet<>();

	Component(Consumer<Component<?>> register, I instance, Runnable init, Runnable close,
			Set<Dependency<?>> dependencies, Set<Class<? super I>> classes) {
		assert instance != null;
		assert init != null;
		assert close != null;
		assert dependencies != null;
		assert classes != null;

		this.instance = instance;
		this.init = init;
		this.close = close;

		// types
		Map<Class<? super I>, PublishedType<? super I>> types = new HashMap<>(classes.size());
		for (Class<? super I> clss : classes) {
//			if (!clss.isAssignableFrom(instance.getClass()))
//				throw new IllegalArgumentException(
//						"Type " + clss.getName() + " is not compatible with " + instance.getClass().getName());
			types.put(clss, new PublishedType<>(this, clss));
		}
		this.types = Collections.unmodifiableMap(types);

		// dependencies
		this.dependencies = Collections.unmodifiableSet(new HashSet<>(dependencies));
		for (Dependency<?> dependency : this.dependencies) {
			dependency.setDependantComponent(this);
		}

		// deactivated by default
		deactivated = CompletableFuture.completedFuture(null);
		deactivationStarted = CompletableFuture.completedFuture(null);

		// TODO check whether context is active, so that we start right away
		prepareNextActivation();

		register.accept(this);
	}

	private void prepareNextActivation() {
		activationStarted = new CompletableFuture<Void>();
		activated = activationStarted //
				.thenComposeAsync(this::dependenciesActivated) //
				.thenRun(this.init) //
				.thenRun(() -> prepareNextDeactivation());
	}

	private void prepareNextDeactivation() {
		deactivationStarted = new CompletableFuture<Void>();
		deactivated = deactivationStarted //
				.thenComposeAsync(this::dependantsDeactivated) //
				.thenRun(this.close) //
				.thenRun(() -> prepareNextActivation());
	}

	public CompletableFuture<Void> getActivated() {
		return activated;
	}

	public CompletableFuture<Void> getDeactivated() {
		return deactivated;
	}

	void startActivating() {
		if (activated.isDone() || activationStarted.isDone())
			return;
		activationStarted.complete(null);
	}

	void startDeactivating() {
		if (deactivated.isDone() || deactivationStarted.isDone())
			return;
		deactivationStarted.complete(null);
	}

	CompletableFuture<Void> dependenciesActivated(Void v) {
		Set<CompletableFuture<?>> constraints = new HashSet<>(this.dependencies.size());
		for (Dependency<?> dependency : this.dependencies) {
			CompletableFuture<Void> dependencyActivated = dependency.publisherActivated() //
					.thenCompose(dependency::set);
			constraints.add(dependencyActivated);
		}
		return CompletableFuture.allOf(constraints.toArray(new CompletableFuture[constraints.size()]));
	}

	CompletableFuture<Void> dependantsDeactivated(Void v) {
		Set<CompletableFuture<?>> constraints = new HashSet<>(this.dependants.size());
		for (Dependency<?> dependant : this.dependants) {
			CompletableFuture<Void> dependantDeactivated = dependant.dependantDeactivated() //
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

	I getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	<T> PublishedType<T> getType(Class<T> clss) {
		if (!types.containsKey(clss))
			throw new IllegalArgumentException(clss.getName() + " is not a type published by this component");
		return (PublishedType<T>) types.get(clss);
	}

	<T> boolean isPublishedType(Class<T> clss) {
		return types.containsKey(clss);
	}

	public static class PublishedType<T> {
		private Component<? extends T> component;
		private Class<T> clss;

//		private CompletableFuture<Component<? extends T>> publisherAvailable;
		private CompletableFuture<T> value;

		public PublishedType(Component<? extends T> component, Class<T> clss) {
			this.clss = clss;
			this.component = component;
			value = CompletableFuture.completedFuture((T) component.instance);
//			value = publisherAvailable.thenApply((c) -> c.getInstance());
		}

		Component<?> getPublisher() {
			return component;
		}

//		CompletableFuture<Component<? extends T>> publisherAvailable() {
//			return publisherAvailable;
//		}

		Class<T> getType() {
			return clss;
		}
	}

	public static class Builder<I> {
		private final I instance;

		private Runnable init;
		private Runnable close;

		private Set<Dependency<?>> dependencies = new HashSet<>();
		private Set<Class<? super I>> types = new HashSet<>();

		public Builder(I instance) {
			this.instance = instance;
		}

		public Component<I> build(Consumer<Component<?>> register) {
			// default values
			if (types.isEmpty()) {
				types.add(getInstanceClass());
			}

			if (init == null)
				init = () -> {
				};
			if (close == null)
				close = () -> {
				};

			// instantiation
			Component<I> component = new Component<I>(register, instance, init, close, dependencies, types);
			for (Dependency<?> dependency : dependencies) {
				dependency.type.getPublisher().addDependant(dependency);
			}
			return component;
		}

		public Builder<I> addType(Class<? super I> clss) {
			types.add(clss);
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

		public <D> Builder<I> addDependency(PublishedType<D> type, Consumer<D> set, Consumer<D> unset) {
			dependencies.add(new Dependency<D>(type, set, unset));
			return this;
		}

		public I get() {
			return instance;
		}

		@SuppressWarnings("unchecked")
		private Class<I> getInstanceClass() {
			return (Class<I>) instance.getClass();
		}

	}

	static class Dependency<D> {
		private PublishedType<D> type;
		private Consumer<D> set;
		private Consumer<D> unset;

		// live
		Component<?> dependantComponent;
		CompletableFuture<Void> setStage;
		CompletableFuture<Void> unsetStage;

		public Dependency(PublishedType<D> types, Consumer<D> set, Consumer<D> unset) {
			super();
			this.type = types;
			this.set = set;
			this.unset = unset != null ? unset : (v) -> set.accept(null);
		}

		// live
		void setDependantComponent(Component<?> component) {
			this.dependantComponent = component;
		}

		CompletableFuture<Void> publisherActivated() {
			return type.getPublisher().activated.copy();
		}

		CompletableFuture<Void> dependantDeactivated() {
			return dependantComponent.deactivated.copy();
		}

		CompletableFuture<Void> set(Void v) {
			return type.value.thenAccept(set);
		}

		CompletableFuture<Void> unset(Void v) {
			return type.value.thenAccept(unset);
		}

	}
}
