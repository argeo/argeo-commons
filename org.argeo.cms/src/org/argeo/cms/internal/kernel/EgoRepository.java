package org.argeo.cms.internal.kernel;

import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.Privilege;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.cms.CmsException;
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.jcr.JcrUtils;

/**
 * Make sure each user has a home directory available.
 */
class EgoRepository extends JcrRepositoryWrapper implements KernelConstants {

	/** The home base path. */
//	private String homeBasePath = KernelConstants.DEFAULT_HOME_BASE_PATH;
//	private String usersBasePath = KernelConstants.DEFAULT_USERS_BASE_PATH;
//	private String groupsBasePath = KernelConstants.DEFAULT_GROUPS_BASE_PATH;

	private Set<String> checkedUsers = new HashSet<String>();

	private SimpleDateFormat usersDatePath = new SimpleDateFormat("YYYY/MM");

	private String defaultHomeWorkspace = NodeConstants.HOME_WORKSPACE;
	private String defaultGroupsWorkspace = NodeConstants.SRV_WORKSPACE;
//	private String defaultGuestsWorkspace = NodeConstants.GUESTS_WORKSPACE;
	private final boolean remote;

	public EgoRepository(Repository repository, boolean remote) {
		super(repository);
		this.remote = remote;
		putDescriptor(NodeConstants.CN, NodeConstants.EGO_REPOSITORY);
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
					loginOrCreateWorkspace(defaultHomeWorkspace);
					loginOrCreateWorkspace(defaultGroupsWorkspace);
					return null;
				}

			});
		}
	}

	private void loginOrCreateWorkspace(String workspace) {
		Session adminSession = null;
		try {
			adminSession = JcrUtils.loginOrCreateWorkspace(getRepository(workspace), workspace);
//			JcrUtils.addPrivilege(adminSession, "/", NodeConstants.ROLE_USER, Privilege.JCR_READ);

//			initJcr(adminSession);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot init JCR home", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

//	@Override
//	public Session login(Credentials credentials, String workspaceName)
//			throws LoginException, NoSuchWorkspaceException, RepositoryException {
//		if (workspaceName == null) {
//			return super.login(credentials, getUserHomeWorkspace());
//		} else {
//			return super.login(credentials, workspaceName);
//		}
//	}

	protected String getUserHomeWorkspace() {
		// TODO base on JAAS Subject metadata
		return defaultHomeWorkspace;
	}

	protected String getGroupsWorkspace() {
		// TODO base on JAAS Subject metadata
		return defaultGroupsWorkspace;
	}

//	protected String getGuestsWorkspace() {
//		// TODO base on JAAS Subject metadata
//		return defaultGuestsWorkspace;
//	}

	@Override
	protected void processNewSession(Session session, String workspaceName) {
		String username = session.getUserID();
		if (username == null || username.toString().equals(""))
			return;
		if (session.getUserID().equals(NodeConstants.ROLE_ANONYMOUS))
			return;

		String userHomeWorkspace = getUserHomeWorkspace();
		if (workspaceName == null || !workspaceName.equals(userHomeWorkspace))
			return;

		if (checkedUsers.contains(username))
			return;
		Session adminSession = KernelUtils.openAdminSession(getRepository(workspaceName), workspaceName);
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
//			JcrUtils.mkdirs(adminSession, homeBasePath);
//			JcrUtils.mkdirs(adminSession, groupsBasePath);
			adminSession.save();

//			JcrUtils.addPrivilege(adminSession, homeBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
//			JcrUtils.addPrivilege(adminSession, groupsBasePath, NodeConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot initialize home repository", e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	protected synchronized void syncJcr(Session adminSession, String username) {
		// only in the default workspace
//		if (workspaceName != null)
//			return;
		// skip system users
		if (username.endsWith(NodeConstants.ROLES_BASEDN))
			return;

		try {
			Node userHome = NodeUtils.getUserHome(adminSession, username);
			if (userHome == null) {
//				String homePath = generateUserPath(username);
				String userId = extractUserId(username);
//				if (adminSession.itemExists(homePath))// duplicate user id
//					userHome = adminSession.getNode(homePath).getParent().addNode(JcrUtils.lastPathElement(homePath));
//				else
//					userHome = JcrUtils.mkdirs(adminSession, homePath);
				userHome = adminSession.getRootNode().addNode(userId);
//				userHome.addMixin(NodeTypes.NODE_USER_HOME);
				userHome.addMixin(NodeType.MIX_CREATED);
				userHome.addMixin(NodeType.MIX_TITLE);
				userHome.setProperty(Property.JCR_ID, username);
				// TODO use display name
				userHome.setProperty(Property.JCR_TITLE, userId);
//				userHome.setProperty(NodeNames.LDAP_UID, username);
				adminSession.save();

				JcrUtils.clearAccessControList(adminSession, userHome.getPath(), username);
				JcrUtils.addPrivilege(adminSession, userHome.getPath(), username, Privilege.JCR_ALL);
//				JackrabbitSecurityUtils.denyPrivilege(adminSession, userHome.getPath(), NodeConstants.ROLE_USER,
//						Privilege.JCR_READ);
			}
			if (adminSession.hasPendingChanges())
				adminSession.save();
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(adminSession);
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
		return '/' + userId;
//		int atIndex = userId.indexOf('@');
//		if (atIndex < 0) {
//			return homeBasePath+'/' + userId;
//		} else {
//			return usersBasePath + '/' + usersDatePath.format(new Date()) + '/' + userId;
//		}
	}

	private String extractUserId(String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		return userId;
//		int atIndex = userId.indexOf('@');
//		if (atIndex < 0) {
//			return homeBasePath+'/' + userId;
//		} else {
//			return usersBasePath + '/' + usersDatePath.format(new Date()) + '/' + userId;
//		}
	}

	public void createWorkgroup(LdapName dn) {
		String groupsWorkspace = getGroupsWorkspace();
		Session adminSession = KernelUtils.openAdminSession(getRepository(groupsWorkspace), groupsWorkspace);
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
			newWorkgroup = adminSession.getRootNode().addNode(relPath, NodeType.NT_UNSTRUCTURED);
//			newWorkgroup = JcrUtils.mkdirs(adminSession.getNode(groupsBasePath), relPath, NodeType.NT_UNSTRUCTURED);
//			newWorkgroup.addMixin(NodeTypes.NODE_GROUP_HOME);
			newWorkgroup.addMixin(NodeType.MIX_CREATED);
			newWorkgroup.addMixin(NodeType.MIX_TITLE);
			newWorkgroup.setProperty(Property.JCR_ID, dn.toString());
			newWorkgroup.setProperty(Property.JCR_TITLE, cn);
//			newWorkgroup.setProperty(NodeNames.LDAP_CN, cn);
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
