package org.argeo.security.jackrabbit;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.argeo.ArgeoException;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;

/** Intermediary class in order to have a consistent naming in config files. */
public class ArgeoSecurityManager extends DefaultSecurityManager {
	private Log log = LogFactory.getLog(ArgeoSecurityManager.class);

	@Override
	/** Since this is called once when the session is created, we take the opportunity to synchronize Spring and Jackrabbit users and groups.*/
	public String getUserID(Subject subject, String workspaceName)
			throws RepositoryException {
		long begin = System.currentTimeMillis();

		if (!subject.getPrincipals(SystemPrincipal.class).isEmpty())
			return super.getUserID(subject, workspaceName);

		Authentication authen;
		Set<Authentication> authens = subject
				.getPrincipals(Authentication.class);
		if (authens.size() == 0)
			throw new ArgeoException("No Spring authentication found in "
					+ subject);
		else
			authen = authens.iterator().next();

		UserManager systemUm = getSystemUserManager(workspaceName);

		String userId = authen.getName();
		User user = (User) systemUm.getAuthorizable(userId);
		if (user == null) {
			user = systemUm.createUser(userId, authen.getCredentials()
					.toString(), authen, null);
			log.info(userId + " added as " + user);
		}

		List<String> userGroupIds = new ArrayList<String>();
		for (GrantedAuthority ga : authen.getAuthorities()) {
			Group group = (Group) systemUm.getAuthorizable(ga.getAuthority());
			if (group == null) {
				group = systemUm.createGroup(ga.getAuthority(),
						new GrantedAuthorityPrincipal(ga), null);
				log.info(ga.getAuthority() + " added as " + group);
			}
			if (!group.isMember(user))
				group.addMember(user);
			userGroupIds.add(ga.getAuthority());
		}

		// check if user has not been removed from some groups
		for (Iterator<Group> it = user.declaredMemberOf(); it.hasNext();) {
			Group group = it.next();
			if (!userGroupIds.contains(group.getID()))
				group.removeMember(user);
		}

		if (log.isTraceEnabled())
			log.trace("Spring and Jackrabbit Security synchronized for user "
					+ userId + " in " + (System.currentTimeMillis() - begin)
					+ " ms");
		return userId;
	}

	@Override
	protected WorkspaceAccessManager createDefaultWorkspaceAccessManager() {
		WorkspaceAccessManager wam = super
				.createDefaultWorkspaceAccessManager();
		return new ArgeoWorkspaceAccessManagerImpl(wam);
	}

	private class ArgeoWorkspaceAccessManagerImpl implements SecurityConstants,
			WorkspaceAccessManager {
		private final WorkspaceAccessManager wam;
		private String defaultWorkspace;

		public ArgeoWorkspaceAccessManagerImpl(WorkspaceAccessManager wam) {
			super();
			this.wam = wam;
		}

		public void init(Session systemSession) throws RepositoryException {
			wam.init(systemSession);
			defaultWorkspace = ((RepositoryImpl) getRepository()).getConfig()
					.getDefaultWorkspaceName();
		}

		public void close() throws RepositoryException {
		}

		public boolean grants(Set<Principal> principals, String workspaceName)
				throws RepositoryException {
			// anonymous has access to the default workspace (required for
			// remoting which does a default login when initializing the
			// repository)
			Boolean anonymous = false;
			for (Principal principal : principals)
				if (principal instanceof AnonymousPrincipal)
					anonymous = true;

			if (anonymous && workspaceName.equals(defaultWorkspace))
				return true;
			else
				return wam.grants(principals, workspaceName);
		}
	}

}
