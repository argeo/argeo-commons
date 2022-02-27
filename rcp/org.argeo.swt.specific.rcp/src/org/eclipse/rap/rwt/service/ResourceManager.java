package org.eclipse.rap.rwt.service;

import java.io.InputStream;

public interface ResourceManager {
	public void register(String name, InputStream in);

	boolean unregister(String name);

	public InputStream getRegisteredContent(String name);

	public String getLocation(String name);

	public boolean isRegistered(String name);
}
