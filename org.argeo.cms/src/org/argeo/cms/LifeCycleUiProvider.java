package org.argeo.cms;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/** CmsUiProvider notified of initialisation with a system session. */
public interface LifeCycleUiProvider extends CmsUiProvider {
	public void init(Session adminSession) throws RepositoryException;

	public void destroy();
}
