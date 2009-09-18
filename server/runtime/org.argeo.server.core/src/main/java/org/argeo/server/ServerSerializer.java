package org.argeo.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ServerSerializer {
	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response);
}
