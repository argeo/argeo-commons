package org.argeo.server;

import java.io.Writer;

public interface Serializer {
	/** Will be removed soon. Use {@link #serialize(Object, Writer)} instead. */
	@Deprecated
	public void serialize(Writer writer, Object obj);

	public void serialize(Object obj, Writer writer);
}
