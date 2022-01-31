package org.argeo.api.uuid.libuuid;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.argeo.api.uuid.UuidBinaryUtils;
import org.argeo.api.uuid.UuidFactory;

/**
 * @deprecated Rather use {@link LibuuidFactory}. This is just a proof of
 *             concept that using shared memory in order to limit the JNI
 *             overhead does not yield any significant performance gain. But it
 *             could be an approach for computing and transferring bulk UUIDs
 *             computations in one go, vi
 *             {@link ByteBuffer#allocateDirect(int)}.
 */
public class DirectLibuuidFactory implements UuidFactory {
	static {
		System.loadLibrary("Java_org_argeo_api_uuid_libuuid");
	}

	@Override
	public UUID get() {
		return timeUUID();
	}

	@Override
	public UUID timeUUID() {
		ByteBuffer buf = ByteBuffer.allocateDirect(16);
		timeUUID(buf);
		byte[] arr = new byte[16];
		buf.get(arr);
		return UuidBinaryUtils.fromBytes(arr);
	}

	protected native void timeUUID(ByteBuffer uuidBuf);

	@Override
	public UUID nameUUIDv5(UUID namespace, byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UUID nameUUIDv3(UUID namespace, byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UUID randomUUIDStrong() {
		throw new UnsupportedOperationException();
	}

	@Override
	public UUID randomUUIDWeak() {
		throw new UnsupportedOperationException();
	}

}
