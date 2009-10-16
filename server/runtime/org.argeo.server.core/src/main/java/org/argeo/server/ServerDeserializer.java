package org.argeo.server;

import java.io.Reader;

/** @deprecated use {@link Deserializer} instead */
public interface ServerDeserializer {
	public Object deserialize(Reader str);
}
