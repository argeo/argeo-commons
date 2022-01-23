package org.argeo.api.acr.uuid;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Execute {@link UUID} creations in {@link ForkJoinPool#commonPool()}. The goal
 * is to provide good performance while staying within the parallelism defined
 * for the system, so as to overwhelm it if many UUIDs are requested.
 * Additionally, with regard to time based UUIDs, since we use
 * {@link ConcurrentTimeUuidState}, which maintains one "clock sequence" per
 * thread, we want to limit the number of threads accessing the actual
 * generation method.
 */
public abstract class AbstractAsyncUuidFactory extends AbstractUuidFactory implements AsyncUuidFactory {
	private SecureRandom secureRandom;
	protected TimeUuidState timeUuidState;

	public AbstractAsyncUuidFactory() {
		secureRandom = newSecureRandom();
		timeUuidState = new ConcurrentTimeUuidState(secureRandom, null);
	}
	/*
	 * ABSTRACT METHODS
	 */

	protected abstract UUID newTimeUUID();

	protected abstract UUID newTimeUUIDwithMacAddress();

	protected abstract SecureRandom newSecureRandom();

	/*
	 * SYNC OPERATIONS
	 */
	protected UUID newRandomUUIDStrong() {
		return newRandomUUID(secureRandom);
	}

	public UUID randomUUIDWeak() {
		return newRandomUUID(ThreadLocalRandom.current());
	}

	/*
	 * ASYNC OPERATIONS (heavy)
	 */
	protected CompletionStage<UUID> request(ForkJoinTask<UUID> newUuid) {
		return CompletableFuture.supplyAsync(newUuid::invoke).minimalCompletionStage();
	}

	@Override
	public CompletionStage<UUID> requestNameUUIDv5(UUID namespace, byte[] data) {
		return request(futureNameUUIDv5(namespace, data));
	}

	@Override
	public CompletionStage<UUID> requestNameUUIDv3(UUID namespace, byte[] data) {
		return request(futureNameUUIDv3(namespace, data));
	}

	@Override
	public CompletionStage<UUID> requestRandomUUIDStrong() {
		return request(futureRandomUUIDStrong());
	}

	@Override
	public CompletionStage<UUID> requestTimeUUID() {
		return request(futureTimeUUID());
	}

	@Override
	public CompletionStage<UUID> requestTimeUUIDwithMacAddress() {
		return request(futureTimeUUIDwithMacAddress());
	}

	/*
	 * ASYNC OPERATIONS (light)
	 */
	protected ForkJoinTask<UUID> submit(Callable<UUID> newUuid) {
		return ForkJoinTask.adapt(newUuid);
	}

	@Override
	public ForkJoinTask<UUID> futureNameUUIDv5(UUID namespace, byte[] data) {
		return submit(() -> newNameUUIDv5(namespace, data));
	}

	@Override
	public ForkJoinTask<UUID> futureNameUUIDv3(UUID namespace, byte[] data) {
		return submit(() -> newNameUUIDv3(namespace, data));
	}

	@Override
	public ForkJoinTask<UUID> futureRandomUUIDStrong() {
		return submit(this::newRandomUUIDStrong);
	}

	@Override
	public ForkJoinTask<UUID> futureTimeUUID() {
		return submit(this::newTimeUUID);
	}

	@Override
	public ForkJoinTask<UUID> futureTimeUUIDwithMacAddress() {
		return submit(this::newTimeUUIDwithMacAddress);
	}

//	@Override
//	public UUID timeUUID() {
//		if (ConcurrentTimeUuidState.isTimeUuidThread.get())
//			return newTimeUUID();
//		else
//			return futureTimeUUID().join();
//	}
//
//	@Override
//	public UUID timeUUIDwithMacAddress() {
//		if (ConcurrentTimeUuidState.isTimeUuidThread.get())
//			return newTimeUUIDwithMacAddress();
//		else
//			return futureTimeUUIDwithMacAddress().join();
//	}

}
