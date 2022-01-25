package org.argeo.api.uuid;

import java.nio.charset.Charset;
import java.util.UUID;

/** A name {@link UUID} whose values used for its construction are known. */
public class NameUuid extends UnkownNameUuid {
	private static final long serialVersionUID = APM.SERIAL;

	protected final TypedUuid namespace;
	protected final String name;
	protected final Charset encoding;

	protected NameUuid(UUID uuid, TypedUuid namespace, String name, Charset encoding) {
		super(uuid);
		assert namespace != null;
		assert name != null;
		assert encoding != null;
		this.namespace = namespace;
		this.name = name;
		this.encoding = encoding;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
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
