package org.argeo.security.jackrabbit;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.security.authorization.acl.ACLProvider;

/** Argeo specific access control provider */
public class ArgeoAccessControlProvider extends ACLProvider {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Session systemSession, Map configuration)
			throws RepositoryException {
		if (!configuration.containsKey(PARAM_ALLOW_UNKNOWN_PRINCIPALS))
			configuration.put(PARAM_ALLOW_UNKNOWN_PRINCIPALS, true);
		super.init(systemSession, configuration);
	}

}
