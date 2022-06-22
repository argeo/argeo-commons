package org.argeo.util.transaction;

import javax.transaction.xa.XAResource;

public interface XAResourceProvider {
	XAResource getXaResource();
}
