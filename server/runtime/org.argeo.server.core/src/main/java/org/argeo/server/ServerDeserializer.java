package org.argeo.server;

import javax.servlet.http.HttpServletRequest;

public interface ServerDeserializer {
	public Object deserialize(Object obj, HttpServletRequest request);
}
