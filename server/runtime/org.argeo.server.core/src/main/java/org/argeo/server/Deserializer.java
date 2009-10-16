package org.argeo.server;

import java.io.Reader;

public interface Deserializer {
	public <T> T deserialize(Reader reader, Class<T> clss);
}
