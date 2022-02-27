package org.argeo.api.uuid;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for objects which are explicitly typed, based on the various
 * variant 2 (RFC 4122) UUID versions (and variant 6 with {@link GUID}, for
 * completion). Such a derivation hierarchy still represents the {@link UUID}
 * itself, not the objects, data or concept that it identifies. Just like
 * {@link UUID}s, {@link TypedUuid} should be used as identifier, not as base
 * class for complex objects. It should rather be seen as a framework to build
 * richer IDs, which are strictly compliant with the UUID specifications.
 */
public abstract class TypedUuid extends UuidHolder {
	private static final long serialVersionUID = APM.SERIAL;

	/** Default constructor. */
	public TypedUuid(UUID uuid) {
		super(uuid);
	}

	/**
	 * Whether this {@link UUID} has no meaning in itself (RFC4122 v3, v4 and v5,
	 * and Microsoft GUID). Only RFC4122 v1 and v2 can be interpreted.
	 */
	public boolean isOpaque() {
		if (uuid.variant() == 2) {// RFC4122
			return uuid.version() == 4 || uuid.version() == 5 || uuid.version() == 3;
		} else if (uuid.variant() == 6) {// Microsoft
			return true;
		} else {
			return true;
		}
	}

	/**
	 * Constructs a {@link TypedUuid} of the most appropriate subtype, based on this
	 * {@link UUID}. For name based UUIDs, it will return an opaque
	 * {@link BasicNameUuid}; {@link NameUuid} and {@link BinaryNameUuid} may be
	 * more useful.
	 */
	public static TypedUuid of(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		if (uuid.variant() == 2) {// RFC 4122
			switch (uuid.version()) {
			case 1:
				return new TimeUuid(uuid);
			case 4:
				return new RandomUuid(uuid);
			case 3:
			case 5:
				return new BasicNameUuid(uuid);
			default:
				throw new IllegalArgumentException("UUIDs with version " + uuid.version() + " are not supported.");
			}
		} else if (uuid.variant() == 6) {// GUID
			return new GUID(uuid);
		} else {
			throw new IllegalArgumentException("UUIDs with variant " + uuid.variant() + " are not supported.");
		}
	}

}
