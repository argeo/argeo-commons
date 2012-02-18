package org.argeo.jackrabbit;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.jcr.security.JcrAuthorizations;

/** Apply authorizations to a Jackrabbit repository. */
public class JackrabbitAuthorizations extends JcrAuthorizations {
	private final static Log log = LogFactory
			.getLog(JackrabbitAuthorizations.class);

	private List<String> groupPrefixes = new ArrayList<String>();

	@Override
	protected Principal getOrCreatePrincipal(Session session,
			String principalName) throws RepositoryException {
		UserManager um = ((JackrabbitSession) session).getUserManager();
		Authorizable authorizable = um.getAuthorizable(principalName);
		if (authorizable == null) {
			groupPrefixes: for (String groupPrefix : groupPrefixes) {
				if (principalName.startsWith(groupPrefix)) {
					authorizable = um.createGroup(principalName);
					log.info("Created group " + principalName);
					break groupPrefixes;
				}
			}
			if (authorizable == null)
				throw new ArgeoException("Authorizable " + principalName
						+ " not found");
		}
		return authorizable.getPrincipal();
	}

	public void setGroupPrefixes(List<String> groupsToCreate) {
		this.groupPrefixes = groupsToCreate;
	}
}
