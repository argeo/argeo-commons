package org.argeo.api.acr.uuid;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * A provider of RFC 4122 {@link UUID}s. Only the RFC 4122 variant (also known
 * as Leach–Salz variant) is supported. The default, returned by the
 * {@link Supplier#get()} method MUST be a v4 UUID (random).
 * 
 * @see UUID
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public interface UuidFactory extends Supplier<UUID> {
	/*
	 * TIME-BASED (version 1)
	 */

	UUID timeUUID();

	UUID timeUUIDwithMacAddress();

	/*
	 * NAME BASED (version 3 and 5)
	 */

	UUID nameUUIDv5(UUID namespace, byte[] name);

	UUID nameUUIDv3(UUID namespace, byte[] name);

	default UUID nameUUIDv5(UUID namespace, String name) {
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null");
		return nameUUIDv5(namespace, name.getBytes(UTF_8));
	}

	default UUID nameUUIDv3(UUID namespace, String name) {
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null");
		return nameUUIDv3(namespace, name.getBytes(UTF_8));
	}

	/*
	 * RANDOM v4
	 */
	UUID randomUUID(Random random);

	default UUID randomUUID() {
		return UUID.randomUUID();
	}

	default UUID randomUUIDWeak() {
		return randomUUID(ThreadLocalRandom.current());
	}

	@Override
	default UUID get() {
		return randomUUID();
	}

	/*
	 * STANDARD UUIDs
	 */

	/** Nil UUID (00000000-0000-0000-0000-000000000000). */
	final static UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	/**
	 * Standard DNS namespace ID for type 3 or 5 UUID (as defined in Appendix C of
	 * RFC4122).
	 */
	final static UUID NS_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard URL namespace ID for type 3 or 5 UUID (as defined in Appendix C of
	 * RFC4122).
	 */
	final static UUID NS_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard OID namespace ID (typically an LDAP type) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	final static UUID NS_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard X500 namespace ID (typically an LDAP DN) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	final static UUID NS_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

	/*
	 * UTILITIES
	 */

	static boolean isRandom(UUID uuid) {
		return uuid.version() == 4;
	}

	static boolean isTimeBased(UUID uuid) {
		return uuid.version() == 1;
	}

	/**
	 * Whether this UUID is time based but was not generated from an IEEE 802
	 * address, as per Section 4.5 of RFC4122.
	 * 
	 * @see https://datatracker.ietf.org/doc/html/rfc4122#section-4.5
	 */
	static boolean isTimeBasedWithMacAddress(UUID uuid) {
		if (uuid.version() == 1) {
			return (uuid.node() & 1L) == 0;
		} else
			return false;
	}

	static boolean isNameBased(UUID uuid) {
		return uuid.version() == 3 || uuid.version() == 5;
	}
}
