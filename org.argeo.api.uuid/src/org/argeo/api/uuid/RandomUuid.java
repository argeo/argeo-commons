package org.argeo.api.uuid;

import java.util.UUID;

/** An opaque variant 2 random {@link UUID} (v4). */
public final class RandomUuid extends TypedUuid {
	private static final long serialVersionUID = APM.SERIAL;

	/** Constructor based on a {@link UUID}. */
	public RandomUuid(UUID uuid) {
		super(uuid);
		if (uuid.version() != 4 && uuid.variant() != 2)
			throw new IllegalArgumentException("The provided UUID is not a time-based UUID.");
	}

	/**
	 * Always returns <code>true</code> since random UUIDs are by definition not
	 * opaque.
	 */
	@Override
	public final boolean isOpaque() {
		return true;
	}

	/** Creates a new {@link RandomUuid} using {@link UUID#randomUUID()}. */
	public static RandomUuid newRandomUuid() {
		return new RandomUuid(UUID.randomUUID());
	}

}
