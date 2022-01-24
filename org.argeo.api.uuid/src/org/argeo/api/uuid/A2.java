package org.argeo.api.uuid;

import java.io.Serializable;

/** A2 metadata for this package. */
class A2 implements Serializable {
	static final int MAJOR = 2;
	static final int MINOR = 3;

	static final long serialVersionUID = (long) MAJOR << 32 | MINOR & 0xFFFFFFFFL;

	static {
//		assert MAJOR == (int) (serialVersionUID >> 32);
//		assert MINOR == (int) serialVersionUID;
	}
}
