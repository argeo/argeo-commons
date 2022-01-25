package org.argeo.api.uuid;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An immutable wrapper for an {@link UUID}, which can be used as a base for a
 * derivation hierarchy, while strongly enforcing semantic equality with the
 * underlying {@link UUID}. It is therefore immutable, and all base methods are
 * directly and trivially based on {@link UUID} methods; they do represent the
 * same unique "thing" (be it an entity, a point in time, etc.), consistently
 * with the fundamental concept of uuid.
 */
public class UuidHolder implements Supplier<UUID>, Serializable {
	private static final long serialVersionUID = APM.SERIAL;

	/**
	 * The wrapped {@link UUID}. Protected rather than private, since it is
	 * immutable and a {@link UUID} is itself immutable.
	 */
	protected final UUID uuid;

	/**
	 * Constructs a new {@link UuidHolder} based on this uuid.
	 * 
	 * @param uuid the UUID to wrap, cannot be null.
	 * @throws NullPointerException if the provided uuid is null.
	 */
	protected UuidHolder(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		this.uuid = uuid;
	}

	/** The wrapped {@link UUID}. */
	public final UUID getUuid() {
		return uuid;
	}

	/** The wrapped {@link UUID}. */
	@Override
	public final UUID get() {
		return getUuid();
	}

	/** Calls {@link UUID#hashCode()} on the wrapped {@link UUID}. */
	@Override
	public final int hashCode() {
		return uuid.hashCode();
	}

	/**
	 * Equals only with non-null {@link UuidHolder} if and only if their wrapped
	 * uuid are equals by calling {@link UUID#equals(Object)}.
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj == null || !(obj instanceof UuidHolder))
			return false;
		UuidHolder typedUuid = (UuidHolder) obj;
		return uuid.equals(typedUuid.uuid);
	}

	/** Calls {@link UUID#toString()} on the wrapped {@link UUID}. */
	@Override
	public final String toString() {
		return uuid.toString();
	}

}
