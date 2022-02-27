package org.argeo.api.uuid;

import java.util.UUID;

/**
 * A name UUID whose binary data used for its construction is known. A new byte
 * array is created and it is copied when retrieved, so this would be
 * inefficient and memory consuming for big data amounts. This rather meant to
 * be used for small binary data, such as certificates, etc.
 */
public class BinaryNameUuid extends BasicNameUuid {
	private static final long serialVersionUID = APM.SERIAL;

	protected final TypedUuid namespace;
	protected final byte[] bytes;

	/** Static builder (a {@link TypedUuidFactory} may be more efficient). */
	public BinaryNameUuid(TypedUuid namespace, byte[] bytes, boolean sha1) {
		this(sha1 ? AbstractUuidFactory.createNameUUIDv5Static(namespace.uuid, bytes)
				: AbstractUuidFactory.createNameUUIDv5Static(namespace.uuid, bytes), namespace, bytes);
	}

	/**
	 * Since no check is performed, the constructor is protected so that the object
	 * can only be built by the default methods of {@link TypedUuidFactory} (in this
	 * package) or by extending the class.
	 */
	protected BinaryNameUuid(UUID uuid, TypedUuid namespace, byte[] bytes) {
		super(uuid);
		this.namespace = namespace;
		this.bytes = new byte[bytes.length];
		System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
	}

	/** The namespace used to build this UUID. */
	public final TypedUuid getNamespace() {
		return namespace;
	}

	/**
	 * A <strong>copy</strong> of the bytes which have been hashed. In order to
	 * access the byte array directly, the class must be extended.
	 */
	public final byte[] getBytes() {
		byte[] bytes = new byte[this.bytes.length];
		System.arraycopy(this.bytes, 0, bytes, 0, this.bytes.length);
		return bytes;
	}

	/** Always returns <code>false</code> since the construction value is known. */
	@Override
	public final boolean isOpaque() {
		return false;
	}

}
