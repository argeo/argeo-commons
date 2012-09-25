package org.argeo.security.jackrabbit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.jcr.JcrSecurityModel;

/** Make sure that user authorizable exists before syncing user directories. */
public class JackrabbitSecurityModel extends JcrSecurityModel {
	private final static Log log = LogFactory
			.getLog(JackrabbitSecurityModel.class);

	@Override
	public Node sync(Session session, String username) {
		User user = null;
		try {
			if (session instanceof JackrabbitSession) {
				UserManager userManager = ((JackrabbitSession) session)
						.getUserManager();
				user = (User) userManager.getAuthorizable(username);
				if (user != null) {
					String principalName = user.getPrincipal().getName();
					if (!principalName.equals(username)) {
						log.warn("Jackrabbit principal is '" + principalName
								+ "' but username is '" + username
								+ "'. Recreating...");
						user.remove();
						user = userManager.createUser(username, "");
					}
				} else {
					// create new principal
					userManager.createUser(username, "");
				}
			}
			Node userProfile = super.sync(session, username);
			if (user != null && userProfile != null) {
				Boolean enabled = userProfile.getProperty(
						ArgeoNames.ARGEO_ENABLED).getBoolean();
				if (enabled && user.isDisabled())
					user.disable(null);
				else if (!enabled && !user.isDisabled())
					user.disable(userProfile.getPath() + " is disabled");
			}
			return userProfile;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot perform Jackrabbit specific operations", e);
		}
	}
}
