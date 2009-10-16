package org.argeo.server;

import java.io.Writer;

public interface Serializer {
	public void serialize(Writer writer, Object obj);
}
