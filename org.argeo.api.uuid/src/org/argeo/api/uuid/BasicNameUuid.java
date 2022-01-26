package org.argeo.api.uuid;

import java.util.UUID;

/** A name based UUID (v3 or v5) whose construction values are not known. */
public class BasicNameUuid extends TypedUuid {
	private static final long serialVersionUID = APM.SERIAL;

	public BasicNameUuid(UUID uuid) {
		super(uuid);
		if ((uuid.version() != 5 && uuid.version() != 3) || uuid.variant() != 2)
			throw new IllegalArgumentException("The provided UUID is not a name-based UUID.");
	}

	/**
	 * Always returns <code>true</true> since it is unknown from which values it was
	 * constructed..
	 */
	@Override
	public boolean isOpaque() {
		return true;
	}

	/**
	 * Whether the hash of this name UUID was generated with SHA-1 (v5) or with MD5
	 * (v3).
	 */
	public boolean isSha1() {
		return uuid.version() == 5;
	}
}
