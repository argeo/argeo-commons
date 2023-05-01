package org.argeo.api.uuid;

import java.util.UUID;

/**
 * An {@link UuidFactory} which does not implement any algorithm and returns
 * {@link UnsupportedOperationException} for methods requiring them. Only
 * {@link UuidFactory#get()} and {@link UuidFactory#randomUUID()} are
 * implemented, trivially based on {@link UUID#randomUUID()}. It can be useful
 * as a base class for partial implementations, or when only random
 * {@link UUID}s are needed, but one wants to integrate with this UUID API via
 * {@link UuidFactory}.
 */
public class NoOpUuidFactory implements UuidFactory {
	public static final UuidFactory onlyJavaRandom = new NoOpUuidFactory();

	/** Returns {@link #randomUUID()}. */
	@Override
	public UUID get() {
		return randomUUID();
	}

	/**
	 * Creates a random UUID (v4) with {@link UUID#randomUUID()}.
	 * 
	 * @return a random {@link UUID}
	 */
	@Override
	public UUID randomUUID() {
		return UUID.randomUUID();
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public UUID timeUUID() {
		throw new UnsupportedOperationException("Time based UUIDs are not implemented");
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public UUID nameUUIDv5(UUID namespace, byte[] data) {
		throw new UnsupportedOperationException("Name based UUIDs are not implemented");
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public UUID nameUUIDv3(UUID namespace, byte[] data) {
		throw new UnsupportedOperationException("Name based UUIDs are not implemented");
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public UUID randomUUIDStrong() {
		throw new UnsupportedOperationException("Strong random UUIDs are not implemented");
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 * 
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public UUID randomUUIDWeak() {
		throw new UnsupportedOperationException("Weak random UUIDs are not implemented");
	}

}
