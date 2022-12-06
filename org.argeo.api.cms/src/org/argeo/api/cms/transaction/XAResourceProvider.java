package org.argeo.api.cms.transaction;

import javax.transaction.xa.XAResource;

public interface XAResourceProvider {
	XAResource getXaResource();
}
