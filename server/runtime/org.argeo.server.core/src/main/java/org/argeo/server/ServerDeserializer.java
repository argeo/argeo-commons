package org.argeo.server;

import java.io.Reader;


public interface ServerDeserializer {
	public Object deserialize(Reader str);
}
