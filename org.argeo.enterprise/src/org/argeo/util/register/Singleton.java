package org.argeo.util.register;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class Singleton<T> {
	private final Class<T> clss;
	private final CompletableFuture<T> registrationStage;
	private final List<Consumer<T>> unregistrationHooks = new ArrayList<>();

	public Singleton(Class<T> clss, CompletableFuture<T> registrationStage) {
		this.clss = clss;
		this.registrationStage = registrationStage;
	}

	CompletionStage<T> getRegistrationStage() {
		return registrationStage.minimalCompletionStage();
	}

	public void addUnregistrationHook(Consumer<T> todo) {
		unregistrationHooks.add(todo);
	}

	public Future<T> getValue() {
		return registrationStage.copy();
	}

	public CompletableFuture<Void> prepareUnregistration(Void v) {
		List<CompletableFuture<Void>> lst = new ArrayList<>();
		for (Consumer<T> hook : unregistrationHooks) {
			lst.add(registrationStage.thenAcceptAsync(hook));
		}
		CompletableFuture<Void> prepareUnregistrationStage = CompletableFuture
				.allOf(lst.toArray(new CompletableFuture<?>[lst.size()]));
		return prepareUnregistrationStage;
	}
}
