package org.argeo.security.jackrabbit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.security.jcr.JcrSecurityModel;
import org.argeo.util.security.SimplePrincipal;

/** Make sure that user authorizable exists before syncing user directories. */
public class JackrabbitSecurityModel extends JcrSecurityModel {

	@Override
	public Node sync(Session session, String username) {
		try {
			if (session instanceof JackrabbitSession) {
				UserManager userManager = ((JackrabbitSession) session)
						.getUserManager();
				User user = (User) userManager
						.getAuthorizable(new SimplePrincipal(username));
				if (user == null)
					userManager.createUser(username, "");
			}
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot perform Jackrabbit specific operaitons", e);
		}
		return super.sync(session, username);
	}

}
