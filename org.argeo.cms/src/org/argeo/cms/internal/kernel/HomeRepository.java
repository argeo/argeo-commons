package org.argeo.cms.internal.kernel;

import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
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
	private String usersBasePath = KernelConstants.DEFAULT_USERS_BASE_PATH;
	private String groupsBasePath = KernelConstants.DEFAULT_GROUPS_BASE_PATH;

	private Set<String> checkedUsers = new HashSet<String>();

	private SimpleDateFormat usersDatePath = new SimpleDateFormat("YYYY/MM");

	private final boolean remote;

	public HomeRepository(Repository repository, boolean remote) {
		super(repository);
		this.remote = remote;
		putDescriptor(NodeConstants.CN, NodeConstants.HOME);
		if (!remote) {
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
	}

	@Override
	protected void processNewSession(Session session) {
		String username = session.getUserID();
		if (username == null || username.toString().equals(""))
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
			JcrUtils.mkdirs(adminSession, groupsBasePath);
			adminSession.save();

			JcrUtils.addPrivilege(adminSession, homeBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
			JcrUtils.addPrivilege(adminSession, groupsBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot initialize home repository", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void syncJcr(Session session, String username) {
		try {
			Node userHome = NodeUtils.getUserHome(session, username);
			if (userHome == null) {
				String homePath = generateUserPath(username);
				if (session.itemExists(homePath))// duplicate user id
					userHome = session.getNode(homePath).getParent().addNode(JcrUtils.lastPathElement(homePath));
				else
					userHome = JcrUtils.mkdirs(session, homePath);
				// userHome = JcrUtils.mkfolders(session, homePath);
				userHome.addMixin(NodeTypes.NODE_USER_HOME);
				userHome.addMixin(NodeType.MIX_CREATED);
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
	private String generateUserPath(String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		int atIndex = userId.indexOf('@');
		if (atIndex < 0) {
			return homeBasePath + '/' + userId;
		} else {
			return usersBasePath + '/' + usersDatePath.format(new Date()) + '/' + userId;
		}
		// if (atIndex > 0) {
		// String domain = userId.substring(0, atIndex);
		// String name = userId.substring(atIndex + 1);
		// return base + '/' + domain + '/' + name;
		// } else if (atIndex == 0 || atIndex == (userId.length() - 1)) {
		// throw new CmsException("Unsupported username " + userId);
		// } else {
		// return base + '/' + userId;
		// }
	}

	public void createWorkgroup(LdapName dn) {
		Session adminSession = KernelUtils.openAdminSession(this);
		String cn = dn.getRdn(dn.size() - 1).getValue().toString();
		Node newWorkgroup = NodeUtils.getGroupHome(adminSession, cn);
		if (newWorkgroup != null) {
			JcrUtils.logoutQuietly(adminSession);
			throw new CmsException("Workgroup " + newWorkgroup + " already exists for " + dn);
		}
		try {
			// TODO enhance transformation of cn to a valid node name
			// String relPath = cn.replaceAll("[^a-zA-Z0-9]", "_");
			String relPath = JcrUtils.replaceInvalidChars(cn);
			newWorkgroup = JcrUtils.mkdirs(adminSession.getNode(groupsBasePath), relPath, NodeType.NT_UNSTRUCTURED);
			newWorkgroup.addMixin(NodeTypes.NODE_GROUP_HOME);
			newWorkgroup.addMixin(NodeType.MIX_CREATED);
			newWorkgroup.setProperty(NodeNames.LDAP_CN, cn);
			adminSession.save();
			JcrUtils.addPrivilege(adminSession, newWorkgroup.getPath(), dn.toString(), Privilege.JCR_ALL);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot create workgroup", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}

	}

	public boolean isRemote() {
		return remote;
	}

}
