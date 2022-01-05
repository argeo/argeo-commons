package org.eclipse.rap.rwt.service;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceLoader {
	public InputStream getResourceAsStream(String resourceName)
			throws IOException;
}
