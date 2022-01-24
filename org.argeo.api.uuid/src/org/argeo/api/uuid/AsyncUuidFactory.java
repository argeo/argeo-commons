package org.argeo.api.uuid;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinTask;

/** A {@link UUID} factory which creates the UUIDs asynchronously. */
public interface AsyncUuidFactory extends UuidFactory {
	/*
	 * TIME-BASED (version 1)
	 */
	CompletionStage<UUID> requestTimeUUID();

	CompletionStage<UUID> requestTimeUUIDwithMacAddress();

	ForkJoinTask<UUID> futureTimeUUID();

	ForkJoinTask<UUID> futureTimeUUIDwithMacAddress();

	/*
	 * NAME BASED (version 3 and 5)
	 */
	CompletionStage<UUID> requestNameUUIDv5(UUID namespace, byte[] data);

	CompletionStage<UUID> requestNameUUIDv3(UUID namespace, byte[] data);

	ForkJoinTask<UUID> futureNameUUIDv5(UUID namespace, byte[] data);

	ForkJoinTask<UUID> futureNameUUIDv3(UUID namespace, byte[] data);

	/*
	 * RANDOM (version 4)
	 */
	CompletionStage<UUID> requestRandomUUIDStrong();

	ForkJoinTask<UUID> futureRandomUUIDStrong();

	/*
	 * DEFAULTS
	 */
	@Override
	default UUID randomUUIDStrong() {
		return futureRandomUUIDStrong().invoke();
	}

	@Override
	default UUID timeUUID() {
		return futureTimeUUID().invoke();
	}

	@Override
	default UUID timeUUIDwithMacAddress() {
		return futureTimeUUIDwithMacAddress().invoke();
	}

	@Override
	default UUID nameUUIDv5(UUID namespace, byte[] data) {
		return futureNameUUIDv5(namespace, data).invoke();
	}

	@Override
	default UUID nameUUIDv3(UUID namespace, byte[] data) {
		return futureNameUUIDv3(namespace, data).invoke();
	}
}
