package org.argeo.security.jackrabbit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.security.DefaultAccessManager;

/** Intermediary class in order to have a consistent naming in config files. */
public class ArgeoAccessManager extends DefaultAccessManager {

	@Override
	public boolean canAccess(String workspaceName) throws RepositoryException {
		// TODO Auto-generated method stub
		return super.canAccess(workspaceName);
	}

}
