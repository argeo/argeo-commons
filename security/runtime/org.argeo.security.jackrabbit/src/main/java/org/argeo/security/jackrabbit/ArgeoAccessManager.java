package org.argeo.security.jackrabbit;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.spi.Path;

/**
 * Intermediary class in order to have a consistent naming in config files. Does
 * nothing for the time being, but may in the future.
 */
public class ArgeoAccessManager extends DefaultAccessManager {

	@Override
	public boolean canRead(Path itemPath, ItemId itemId)
			throws RepositoryException {
		return super.canRead(itemPath, itemId);
	}

	@Override
	public Privilege[] getPrivileges(String absPath)
			throws PathNotFoundException, RepositoryException {
		return super.getPrivileges(absPath);
	}

	@Override
	public boolean hasPrivileges(String absPath, Privilege[] privileges)
			throws PathNotFoundException, RepositoryException {
		return super.hasPrivileges(absPath, privileges);
	}

}
