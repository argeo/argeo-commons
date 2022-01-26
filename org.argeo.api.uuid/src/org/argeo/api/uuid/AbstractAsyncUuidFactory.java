package org.argeo.api.uuid;

import java.security.DrbgParameters;
import java.security.DrbgParameters.Capability;
import java.security.SecureRandom;
import java.security.SecureRandomParameters;
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
	protected ConcurrentTimeUuidState timeUuidState;

	private NodeIdSupplier nodeIdSupplier;
	private long currentClockSequenceRange = 0;

	public AbstractAsyncUuidFactory() {
		secureRandom = createSecureRandom();
		timeUuidState = new ConcurrentTimeUuidState(secureRandom, null);
	}
	/*
	 * ABSTRACT METHODS
	 */

	protected abstract SecureRandom createSecureRandom();

	/*
	 * STATE
	 */
	public void reset() {
		if (nodeIdSupplier == null)
			throw new IllegalStateException("No node id supplier available");
		long nodeIdBase = nodeIdSupplier.get();
		timeUuidState.reset(nodeIdBase, currentClockSequenceRange);
	}

	public void setNodeIdSupplier(NodeIdSupplier nodeIdSupplier) {
		this.nodeIdSupplier = nodeIdSupplier;
		reset();
	}

	public void setNodeIdSupplier(NodeIdSupplier nodeIdSupplier, long range) {
		this.currentClockSequenceRange = range >= 0 ? range & 0x3F00 : range;
		setNodeIdSupplier(nodeIdSupplier);
	}

	protected NodeIdSupplier getNodeIdSupplier() {
		return nodeIdSupplier;
	}

	/**
	 * If positive, only clock_hi is taken from the argument (range & 0x3F00), if
	 * negative, the full range of possible values is used.
	 */
	public void setCurrentClockSequenceRange(long range) {
		this.currentClockSequenceRange = range >= 0 ? range & 0x3F00 : range;
		reset();
	}

	/*
	 * SYNC OPERATIONS
	 */
	protected UUID createRandomUUIDStrong() {
		SecureRandomParameters parameters = secureRandom.getParameters();
		if (parameters != null) {
			if (parameters instanceof DrbgParameters.Instantiation) {
				Capability capability = ((DrbgParameters.Instantiation) parameters).getCapability();
				if (capability.equals(DrbgParameters.Capability.PR_AND_RESEED)
						|| capability.equals(DrbgParameters.Capability.RESEED_ONLY)) {
					secureRandom.reseed();
				}
			}
		}
		return createRandomUUID(secureRandom);
	}

	public UUID randomUUIDWeak() {
		return createRandomUUID(ThreadLocalRandom.current());
	}

	protected UUID createTimeUUID() {
		if (nodeIdSupplier == null)
			throw new IllegalStateException("No node id supplier available");
		UUID uuid = new UUID(timeUuidState.getMostSignificantBits(), timeUuidState.getLeastSignificantBits());

		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		assert uuid.timestamp() == timeUuidState.getLastTimestamp();
		assert uuid.clockSequence() == timeUuidState.getClockSequence();

		return uuid;
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

	/*
	 * ASYNC OPERATIONS (light)
	 */
	protected ForkJoinTask<UUID> submit(Callable<UUID> newUuid) {
		return ForkJoinTask.adapt(newUuid);
	}

	@Override
	public ForkJoinTask<UUID> futureNameUUIDv5(UUID namespace, byte[] data) {
		return submit(() -> createNameUUIDv5(namespace, data));
	}

	@Override
	public ForkJoinTask<UUID> futureNameUUIDv3(UUID namespace, byte[] data) {
		return submit(() -> createNameUUIDv3(namespace, data));
	}

	@Override
	public ForkJoinTask<UUID> futureRandomUUIDStrong() {
		return submit(this::createRandomUUIDStrong);
	}

	@Override
	public ForkJoinTask<UUID> futureTimeUUID() {
		return submit(this::createTimeUUID);
	}
}
