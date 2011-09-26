package org.argeo.jcr.proxy;

import javax.jcr.Node;
import javax.jcr.Session;

/** A proxy which nows how to resolve and synchronize relative URLs */
public interface ResourceProxy {
	/** Path to the proxied node (which may not already exist) */
	public String getNodePath(String relativePath);

	/**
	 * Proxy the file referenced by this relative path in the underlying
	 * repository
	 * 
	 * @return the unique identifier of the proxied Node, <code>null</code> if
	 *         the resource was not found (e.g. HTPP 404)
	 */
	public Node proxy(Session session,String relativePath);
}
