package org.argeo.api.cms.transaction;

import javax.transaction.xa.XAResource;

/**
 * A minimalistic interface similar to OSGi transaction context in order to
 * register XA resources.
 */
public interface WorkContext {
	void registerXAResource(XAResource resource, String recoveryId);
}
