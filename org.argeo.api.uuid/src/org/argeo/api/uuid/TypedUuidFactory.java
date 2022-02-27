package org.argeo.api.uuid;

import java.nio.charset.Charset;

/** An {@link UuidFactory} which also (trivially) produces {@link TypedUuid}. */
public interface TypedUuidFactory extends UuidFactory {
	/** Creates a {@link TimeUuid} (v1). */
	default TimeUuid newTimeUuid() {
		return new TimeUuid(timeUUID());
	}

	/** Creates an MD5 {@link NameUuid} (v3). */
	default NameUuid newNameUuidV3(TypedUuid namespace, String name, Charset charset) {
		return new NameUuid(nameUUIDv3(namespace.get(), name, charset), namespace, name, charset);
	}

	/** Creates a {@link RandomUuid}, using {@link #randomUUID()}. */
	default RandomUuid newRandomUuid() {
		return new RandomUuid(randomUUID());
	}

	/** Creates an SHA1 {@link NameUuid} (v5). */
	default NameUuid newNameUuidV5(TypedUuid namespace, String name, Charset charset) {
		return new NameUuid(nameUUIDv5(namespace.get(), name, charset), namespace, name, charset);
	}
}
