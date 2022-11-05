package org.argeo.eclipse.ui.rcp.internal.rwt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.argeo.cms.util.StreamUtils;
import org.eclipse.rap.rwt.service.ResourceManager;

public class RcpResourceManager implements ResourceManager {
	private Map<String, byte[]> register = Collections.synchronizedMap(new TreeMap<String, byte[]>());

	@Override
	public void register(String name, InputStream in) {
		try {
			register.put(name, StreamUtils.toByteArray(in));
		} catch (IOException e) {
			throw new RuntimeException("Cannot register " + name, e);
		}
	}

	@Override
	public boolean unregister(String name) {
		return register.remove(name) != null;
	}

	@Override
	public InputStream getRegisteredContent(String name) {
		return new ByteArrayInputStream(register.get(name));
	}

	@Override
	public String getLocation(String name) {
		return name;
	}

	@Override
	public boolean isRegistered(String name) {
		return register.containsKey(name);
	}

}
