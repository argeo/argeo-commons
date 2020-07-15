package org.argeo.jcr.proxy;

import javax.jcr.Node;

/** A proxy which nows how to resolve and synchronize relative URLs */
public interface ResourceProxy {
	/**
	 * Proxy the file referenced by this relative path in the underlying
	 * repository. A new session is created by each call, so the underlying
	 * session of the returned node must be closed by the caller.
	 * 
	 * @return the proxied Node, <code>null</code> if the resource was not found
	 *         (e.g. HTTP 404)
	 */
	public Node proxy(String relativePath);
}
