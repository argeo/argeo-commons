package org.argeo.api.uuid;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Objects;
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
	/**
	 * A new time based {@link UUID} (v1) with efforts to make it unique on this
	 * node.
	 */
	UUID timeUUID();

	/*
	 * NAME BASED (version 3 and 5)
	 */
	/**
	 * A new {@link UUID} v5, which an SHA1 digest of namespace and the provided
	 * bytes. This use to build names and implementation MAY restrict the maximal
	 * size of the byte array.
	 * 
	 * @see UuidFactory#NAMESPACE_UUID_DNS
	 * @see UuidFactory#NAMESPACE_UUID_URL
	 * @see UuidFactory#NAMESPACE_UUID_OID
	 * @see UuidFactory#NAMESPACE_UUID_X500
	 */
	UUID nameUUIDv5(UUID namespace, byte[] data);

	/**
	 * A new {@link UUID} v3, which a MD5 digest of namespace and the provided
	 * bytes. This use to build names and implementation MAY restrict the maximal
	 * size of the byte array.
	 * 
	 * @see UuidFactory#NAMESPACE_UUID_DNS
	 * @see UuidFactory#NAMESPACE_UUID_URL
	 * @see UuidFactory#NAMESPACE_UUID_OID
	 * @see UuidFactory#NAMESPACE_UUID_X500
	 */
	UUID nameUUIDv3(UUID namespace, byte[] data);

	/**
	 * A convenience method to generate a name based UUID v5 based on a string,
	 * using the UTF-8 charset.
	 * 
	 * @see UuidFactory#nameUUIDv5(UUID, byte[])
	 */
	default UUID nameUUIDv5(UUID namespace, String name) {
		return nameUUIDv5(namespace, name, UTF_8);
	}

	/**
	 * A convenience method to generate a name based UUID v5 based on a string.
	 * 
	 * @see UuidFactory#nameUUIDv5(UUID, byte[])
	 */
	default UUID nameUUIDv5(UUID namespace, String name, Charset charset) {
		Objects.requireNonNull(name, "Name cannot be null");
		return nameUUIDv5(namespace, name.getBytes(charset));
	}

	/**
	 * A convenience method to generate a name based UUID v3 based on a string,
	 * using the UTF-8 charset.
	 * 
	 * @see UuidFactory#nameUUIDv3(UUID, byte[])
	 */
	default UUID nameUUIDv3(UUID namespace, String name) {
		return nameUUIDv3(namespace, name, UTF_8);
	}

	/**
	 * A convenience method to generate a name based UUID v3 based on a string.
	 * 
	 * @see UuidFactory#nameUUIDv3(UUID, byte[])
	 */
	default UUID nameUUIDv3(UUID namespace, String name, Charset charset) {
		Objects.requireNonNull(name, "Name cannot be null");
		return nameUUIDv3(namespace, name.getBytes(charset));
	}

	/*
	 * RANDOM (version 4)
	 */
	/**
	 * A random UUID at least as good as {@link UUID#randomUUID()}, but with efforts
	 * to make it even more random, using more secure algorithms and resseeding.
	 */
	UUID randomUUIDStrong();

	/**
	 * An {@link UUID} generated based on {@link ThreadLocalRandom}. Implementations
	 * should always provide it synchronously.
	 */
	UUID randomUUIDWeak();

	/**
	 * The default random {@link UUID} (v4) generator to use. This default
	 * implementation returns {@link #randomUUIDStrong()}. In general, one should
	 * use {@link UUID#randomUUID()} to generate random UUID, as it is certainly the
	 * best balanced and to avoid unnecessary dependencies with an API. The
	 * implementations provided here are either when is looking for something
	 * "stronger" ({@link #randomUUIDStrong()} or faster {@link #randomUUIDWeak()}.
	 */
	default UUID randomUUID() {
		return randomUUIDStrong();
	}

	/**
	 * The default {@link UUID} to provide, either random (v4) or time based (v1).
	 * This default implementations returns {@link #timeUUID()} because it is
	 * supposed to be fast and use few resources.
	 */
	@Override
	default UUID get() {
		return timeUUID();
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
	final static UUID NAMESPACE_UUID_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard URL namespace ID for type 3 or 5 UUID (as defined in Appendix C of
	 * RFC4122).
	 */
	final static UUID NAMESPACE_UUID_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard OID namespace ID (typically an LDAP type) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	final static UUID NAMESPACE_UUID_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard X500 namespace ID (typically an LDAP DN) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	final static UUID NAMESPACE_UUID_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

	/*
	 * UTILITIES
	 */
	/** Whether this {@link UUID} is random (v4). */
	static boolean isRandom(UUID uuid) {
		return uuid.version() == 4;
	}

	/** Whether this {@link UUID} is time based (v1). */
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

	/** Whether this {@link UUID} is name based (v3 or v5). */
	static boolean isNameBased(UUID uuid) {
		return uuid.version() == 3 || uuid.version() == 5;
	}
}
