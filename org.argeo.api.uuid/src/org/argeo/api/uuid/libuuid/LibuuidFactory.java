package org.argeo.api.uuid.libuuid;

import java.util.UUID;

import org.argeo.api.uuid.TypedUuidFactory;
import org.argeo.api.uuid.UuidFactory;

/** An {@link UuidFactory} based on a native library. */
public class LibuuidFactory implements UuidFactory, TypedUuidFactory {
	static {
		System.loadLibrary("Java_org_argeo_api_uuid_libuuid." + APM.MAJOR + "." + APM.MINOR);
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
		return randomUUIDStrong();
	}

}
