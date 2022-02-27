package org.argeo.api.uuid;

import java.io.Serializable;

/** Package metadata for this package. */
class APM implements Serializable {
	/** Major version (equality means backward compatibility). */
	static final int MAJOR = 2;
	/** Minor version (if even, equality means forward compatibility). */
	static final int MINOR = 3;
	/** serialVersionUID to use for {@link Serializable} classes in this package. */
	static final long SERIAL = (long) MAJOR << 32 | MINOR & 0xFFFFFFFFL;
	/** Metadata version. */
	private static final long serialVersionUID = 2L;
}
