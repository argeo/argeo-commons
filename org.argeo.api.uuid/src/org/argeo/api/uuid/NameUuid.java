package org.argeo.api.uuid;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A name UUID whose values used for its construction are known. */
public class NameUuid extends BasicNameUuid {
	private static final long serialVersionUID = APM.SERIAL;

	protected final TypedUuid namespace;
	protected final String name;
	protected final Charset encoding;

	/**
	 * Default static builder which creates a v5 (SHA1) name based {@link UUID},
	 * using UTF-8 encoding. Use
	 * {@link #NameUuid(TypedUuid, String, Charset, boolean)} for more options.
	 */
	public NameUuid(TypedUuid namespace, String name) {
		this(namespace, name, StandardCharsets.UTF_8, true);
	}

	/** Static builder (an {@link TypedUuidFactory} may be more efficient). */
	public NameUuid(TypedUuid namespace, String name, Charset encoding, boolean sha1) {
		this(sha1 ? AbstractUuidFactory.createNameUUIDv5Static(namespace.uuid, name.getBytes(encoding))
				: AbstractUuidFactory.createNameUUIDv5Static(namespace.uuid, name.getBytes(encoding)), namespace, name,
				encoding);
	}

	/**
	 * Since no check is performed, the constructor is protected so that the object
	 * can only be built by the default methods of {@link TypedUuidFactory} (in this
	 * package) or by extending the class.
	 */
	protected NameUuid(UUID uuid, TypedUuid namespace, String name, Charset encoding) {
		super(uuid);
		assert namespace != null;
		assert name != null;
		assert encoding != null;
		this.namespace = namespace;
		this.name = name;
		this.encoding = encoding;
	}

	/** The namespace used to build this UUID. */
	public final TypedUuid getNamespace() {
		return namespace;
	}

	/** The name of this UUID. */
	public final String getName() {
		return name;
	}

	/** The encoding used to create this UUID. */
	public final Charset getEncoding() {
		return encoding;
	}

	/** Always returns <code>false</code> since construction values are known. */
	@Override
	public final boolean isOpaque() {
		return false;
	}

}
