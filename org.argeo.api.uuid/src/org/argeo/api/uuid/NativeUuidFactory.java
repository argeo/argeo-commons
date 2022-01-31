package org.argeo.api.uuid;

import java.util.UUID;

/** An {@link UuidFactory} based on a native library. */
public class NativeUuidFactory implements UuidFactory, TypedUuidFactory {
	static {
		System.loadLibrary("Java_org_argeo_api_uuid");
	}

	@Override
	public UUID get() {
		return timeUUID();
	}

	@Override
	public native UUID timeUUID();

	@Override
	public native UUID nameUUIDv5(UUID namespace, byte[] data);

	@Override
	public native UUID nameUUIDv3(UUID namespace, byte[] data);

	@Override
	public native UUID randomUUIDStrong();

	@Override
	public UUID randomUUIDWeak() {
		throw new UnsupportedOperationException();
	}

}
