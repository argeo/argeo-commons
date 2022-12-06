package org.argeo.api.cms.transaction;

import java.util.concurrent.Callable;

/**
 * A minimalistic interface inspired by OSGi transaction control in order to
 * commit units of work externally.
 */
public interface WorkControl {
	<T> T required(Callable<T> work);

	void setRollbackOnly();

	WorkContext getWorkContext();
}
