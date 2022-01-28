package org.argeo.jackrabbit.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;

/**
 * Implementation of {@link AuthCache} which doesn't use serialization, as it is
 * not supported by GraalVM at this stage.
 */
public class NonSerialBasicAuthCache implements AuthCache {
	private final Map<HttpHost, AuthScheme> cache;

	public NonSerialBasicAuthCache() {
		cache = new ConcurrentHashMap<HttpHost, AuthScheme>();
	}

	@Override
	public void put(HttpHost host, AuthScheme authScheme) {
		cache.put(host, authScheme);
	}

	@Override
	public AuthScheme get(HttpHost host) {
		return cache.get(host);
	}

	@Override
	public void remove(HttpHost host) {
		cache.remove(host);
	}

	@Override
	public void clear() {
		cache.clear();
	}

}
