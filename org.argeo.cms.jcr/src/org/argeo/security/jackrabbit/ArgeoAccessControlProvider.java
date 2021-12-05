package org.argeo.security.jackrabbit;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.security.authorization.acl.ACLProvider;

/** Argeo specific access control provider */
public class ArgeoAccessControlProvider extends ACLProvider {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Session systemSession, Map configuration) throws RepositoryException {
		if (!configuration.containsKey(PARAM_ALLOW_UNKNOWN_PRINCIPALS))
			configuration.put(PARAM_ALLOW_UNKNOWN_PRINCIPALS, "true");
		if (!configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS))
			configuration.put(PARAM_OMIT_DEFAULT_PERMISSIONS, "true");
		super.init(systemSession, configuration);
	}

	@Override
	public boolean canAccessRoot(Set<Principal> principals) throws RepositoryException {
		return super.canAccessRoot(principals);
	}

}
