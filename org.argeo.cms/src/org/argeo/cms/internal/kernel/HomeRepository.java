package org.argeo.cms.internal.kernel;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.argeo.cms.CmsException;
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeNames;
import org.argeo.node.NodeTypes;
import org.argeo.node.NodeUtils;

/**
 * Make sure each user has a home directory available in the default workspace.
 */
class HomeRepository extends JcrRepositoryWrapper implements KernelConstants {
	/** The home base path. */
	private String homeBasePath = KernelConstants.DEFAULT_HOME_BASE_PATH;
//	private String groupsBasePath = KernelConstants.DEFAULT_GROUPS_BASE_PATH;

	private Set<String> checkedUsers = new HashSet<String>();

	public HomeRepository(Repository repository) {
		super(repository);
		putDescriptor(NodeConstants.CN, NodeConstants.HOME);
		LoginContext lc;
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_DATA_ADMIN);
			lc.login();
		} catch (javax.security.auth.login.LoginException e1) {
			throw new CmsException("Cannot login as systrem", e1);
		}
		Subject.doAs(lc.getSubject(), new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					Session adminSession = getRepository().login();
					initJcr(adminSession);
				} catch (RepositoryException e) {
					throw new CmsException("Cannot init JCR home", e);
				}
				return null;
			}

		});
	}

	@Override
	protected void processNewSession(Session session) {
		String username = session.getUserID();
		if (username == null)
			return;
		if (session.getUserID().equals(NodeConstants.ROLE_ANONYMOUS))
			return;

		if (checkedUsers.contains(username))
			return;
		Session adminSession = KernelUtils.openAdminSession(getRepository(), session.getWorkspace().getName());
		try {
			syncJcr(adminSession, username);
			checkedUsers.add(username);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	/*
	 * JCR
	 */
	/** Session is logged out. */
	private void initJcr(Session adminSession) {
		try {
			JcrUtils.mkdirs(adminSession, homeBasePath);
//			JcrUtils.mkdirs(adminSession, groupsBasePath);
			adminSession.save();

			JcrUtils.addPrivilege(adminSession, homeBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_ALL);
//			JcrUtils.addPrivilege(adminSession, groupsBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_ALL);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot initialize node user admin", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void syncJcr(Session session, String username) {
		try {
			Node userHome = NodeUtils.getUserHome(session, username);
			if (userHome == null) {
				String homePath = generateUserPath(homeBasePath, username);
				if (session.itemExists(homePath))// duplicate user id
					userHome = session.getNode(homePath).getParent().addNode(JcrUtils.lastPathElement(homePath));
				else
					userHome = JcrUtils.mkdirs(session, homePath);
				// userHome = JcrUtils.mkfolders(session, homePath);
				userHome.addMixin(NodeTypes.NODE_USER_HOME);
				userHome.setProperty(NodeNames.LDAP_UID, username);
				session.save();

				JcrUtils.clearAccessControList(session, homePath, username);
				JcrUtils.addPrivilege(session, homePath, username, Privilege.JCR_ALL);
			}
			if (session.hasPendingChanges())
				session.save();
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new CmsException("Cannot sync node security model for " + username, e);
		}
	}

	/** Generate path for a new user home */
	private String generateUserPath(String base, String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		int atIndex = userId.indexOf('@');
		if (atIndex > 0) {
			String domain = userId.substring(0, atIndex);
			String name = userId.substring(atIndex + 1);
			return base + '/' + domain + '/' + name;
		} else if (atIndex == 0 || atIndex == (userId.length() - 1)) {
			throw new CmsException("Unsupported username " + userId);
		} else {
			return base + '/' + userId;
		}
	}

	public String getHomeBasePath() {
		return homeBasePath;
	}

//	public String getGroupsBasePath() {
//		return groupsBasePath;
//	}

}
