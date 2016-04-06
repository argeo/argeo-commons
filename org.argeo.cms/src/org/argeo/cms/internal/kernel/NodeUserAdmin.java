package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;
import static org.argeo.cms.internal.kernel.KernelUtils.getOsgiInstanceDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.osgi.useradmin.UserDirectoryException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with this node
 * system roles.
 */
public class NodeUserAdmin implements UserAdmin, KernelConstants {
	private final static Log log = LogFactory.getLog(NodeUserAdmin.class);
	final static LdapName ROLES_BASE;
	static {
		try {
			ROLES_BASE = new LdapName(AuthConstants.ROLES_BASEDN);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Cannot initialize " + NodeUserAdmin.class, e);
		}
	}

	// DAOs
	private UserAdmin nodeRoles = null;
	private Map<LdapName, UserAdmin> userAdmins = new HashMap<LdapName, UserAdmin>();

	// JCR
	/** The home base path. */
	private String homeBasePath = "/home";
	private String peopleBasePath = ArgeoJcrConstants.PEOPLE_BASE_PATH;
	private Repository repository;
	private Session adminSession;

	private final String cacheName = UserDirectory.class.getName();

	public NodeUserAdmin(TransactionManager transactionManager, Repository repository) {
		this.repository = repository;
		try {
			this.adminSession = this.repository.login();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot log-in", e);
		}

		// DAOs
		File nodeBaseDir = new File(getOsgiInstanceDir(), DIR_NODE);
		nodeBaseDir.mkdirs();
		String userAdminUri = getFrameworkProp(USERADMIN_URIS);
		initUserAdmins(userAdminUri, nodeBaseDir);
		String nodeRolesUri = getFrameworkProp(ROLES_URI);
		initNodeRoles(nodeRolesUri, nodeBaseDir);

		// Transaction manager
		((UserDirectory) nodeRoles).setTransactionManager(transactionManager);
		for (UserAdmin userAdmin : userAdmins.values()) {
			if (userAdmin instanceof UserDirectory)
				((UserDirectory) userAdmin).setTransactionManager(transactionManager);
		}

		// JCR
		initJcr(adminSession);
	}

	Dictionary<String, ?> currentState() {
		Dictionary<String, Object> res = new Hashtable<String, Object>();
		for (LdapName name : userAdmins.keySet()) {
			StringBuilder buf = new StringBuilder();
			if (userAdmins.get(name) instanceof UserDirectory) {
				UserDirectory userDirectory = (UserDirectory) userAdmins.get(name);
				String uri = UserAdminConf.propertiesAsUri(userDirectory.getProperties()).toString();
				res.put(uri, "");
			} else {
				buf.append('/').append(name.toString()).append("?readOnly=true");
			}
		}
		return res;
	}

	public void destroy() {
		for (LdapName name : userAdmins.keySet()) {
			if (userAdmins.get(name) instanceof UserDirectory) {
				UserDirectory userDirectory = (UserDirectory) userAdmins.get(name);
				try {
					// FIXME Make it less bitronix dependant
					EhCacheXAResourceProducer.unregisterXAResource(cacheName, userDirectory.getXaResource());
				} catch (Exception e) {
					log.error("Cannot unregister resource from Bitronix", e);
				}
				userDirectory.destroy();

			}
		}
	}

	@Override
	public Role createRole(String name, int type) {
		return findUserAdmin(name).createRole(name, type);
	}

	@Override
	public boolean removeRole(String name) {
		boolean actuallyDeleted = findUserAdmin(name).removeRole(name);
		nodeRoles.removeRole(name);
		return actuallyDeleted;
	}

	@Override
	public Role getRole(String name) {
		return findUserAdmin(name).getRole(name);
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		List<Role> res = new ArrayList<Role>();
		for (UserAdmin userAdmin : userAdmins.values()) {
			res.addAll(Arrays.asList(userAdmin.getRoles(filter)));
		}
		res.addAll(Arrays.asList(nodeRoles.getRoles(filter)));
		return res.toArray(new Role[res.size()]);
	}

	@Override
	public User getUser(String key, String value) {
		List<User> res = new ArrayList<User>();
		for (UserAdmin userAdmin : userAdmins.values()) {
			User u = userAdmin.getUser(key, value);
			if (u != null)
				res.add(u);
		}
		// Note: node roles cannot contain users, so it is not searched
		return res.size() == 1 ? res.get(0) : null;
	}

	@Override
	public Authorization getAuthorization(User user) {
		if (user == null) {// anonymous
			return nodeRoles.getAuthorization(null);
		}
		UserAdmin userAdmin = findUserAdmin(user.getName());
		Authorization rawAuthorization = userAdmin.getAuthorization(user);
		// gather system roles
		Set<String> systemRoles = new HashSet<String>();
		for (String role : rawAuthorization.getRoles()) {
			Authorization auth = nodeRoles.getAuthorization((User) userAdmin.getRole(role));
			systemRoles.addAll(Arrays.asList(auth.getRoles()));
		}
		Authorization authorization = new NodeAuthorization(rawAuthorization.getName(), rawAuthorization.toString(),
				systemRoles, rawAuthorization.getRoles());
		syncJcr(adminSession, authorization);
		return authorization;
	}

	//
	// USER ADMIN AGGREGATOR
	//
	public void addUserAdmin(String baseDn, UserAdmin userAdmin) {
		if (userAdmins.containsKey(baseDn))
			throw new UserDirectoryException("There is already a user admin for " + baseDn);
		try {
			userAdmins.put(new LdapName(baseDn), userAdmin);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted base DN " + baseDn, e);
		}
		if (userAdmin instanceof UserDirectory) {
			try {
				// FIXME Make it less bitronix dependant
				EhCacheXAResourceProducer.registerXAResource(cacheName, ((UserDirectory) userAdmin).getXaResource());
			} catch (Exception e) {
				log.error("Cannot register resource to Bitronix", e);
			}
		}
	}

	private UserAdmin findUserAdmin(String name) {
		try {
			return findUserAdmin(new LdapName(name));
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted name " + name, e);
		}
	}

	private UserAdmin findUserAdmin(LdapName name) {
		if (name.startsWith(ROLES_BASE))
			return nodeRoles;
		List<UserAdmin> res = new ArrayList<UserAdmin>(1);
		for (LdapName baseDn : userAdmins.keySet()) {
			if (name.startsWith(baseDn))
				res.add(userAdmins.get(baseDn));
		}
		if (res.size() == 0)
			throw new UserDirectoryException("Cannot find user admin for " + name);
		if (res.size() > 1)
			throw new UserDirectoryException("Multiple user admin found for " + name);
		return res.get(0);
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		if (nodeRoles instanceof UserDirectory)
			((UserDirectory) nodeRoles).setTransactionManager(transactionManager);
		for (UserAdmin userAdmin : userAdmins.values()) {
			if (userAdmin instanceof UserDirectory)
				((UserDirectory) userAdmin).setTransactionManager(transactionManager);
		}
	}

	private void initUserAdmins(String userAdminUri, File nodeBaseDir) {
		if (userAdminUri == null) {
			String demoBaseDn = "dc=example,dc=com";
			File businessRolesFile = new File(nodeBaseDir, demoBaseDn + ".ldif");
			if (!businessRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(demoBaseDn + ".ldif"),
							businessRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			userAdminUri = businessRolesFile.toURI().toString();
		}
		String[] uris = userAdminUri.split(" ");
		for (String uri : uris) {
			URI u;
			try {
				u = new URI(uri);
				if (u.getPath() == null)
					throw new CmsException("URI " + uri + " must have a path in order to determine base DN");
				if (u.getScheme() == null) {
					if (uri.startsWith("/") || uri.startsWith("./") || uri.startsWith("../"))
						u = new File(uri).getCanonicalFile().toURI();
					else if (!uri.contains("/")) {
						u = new URI(nodeBaseDir.toURI() + uri);
						// u = new File(nodeBaseDir, uri).getCanonicalFile()
						// .toURI();
					} else
						throw new CmsException("Cannot interpret " + uri + " as an uri");
				} else if (u.getScheme().equals("file")) {
					u = new File(u).getCanonicalFile().toURI();
				}
			} catch (Exception e) {
				throw new CmsException("Cannot interpret " + uri + " as an uri", e);
			}
			Dictionary<String, ?> properties = UserAdminConf.uriAsProperties(u.toString());
			UserDirectory businessRoles;
			if (u.getScheme().startsWith("ldap")) {
				businessRoles = new LdapUserAdmin(properties);
			} else {
				businessRoles = new LdifUserAdmin(properties);
			}
			businessRoles.init();
			String baseDn = businessRoles.getBaseDn();
			if (userAdmins.containsKey(baseDn))
				throw new UserDirectoryException("There is already a user admin for " + baseDn);
			try {
				userAdmins.put(new LdapName(baseDn), (UserAdmin) businessRoles);
			} catch (InvalidNameException e) {
				throw new UserDirectoryException("Badly formatted base DN " + baseDn, e);
			}
			addUserAdmin(businessRoles.getBaseDn(), (UserAdmin) businessRoles);
			if (log.isDebugEnabled())
				log.debug("User directory " + businessRoles.getBaseDn() + " [" + u.getScheme() + "] enabled.");
		}

	}

	private void initNodeRoles(String nodeRolesUri, File nodeBaseDir) {
		String baseNodeRoleDn = AuthConstants.ROLES_BASEDN;
		if (nodeRolesUri == null) {
			File nodeRolesFile = new File(nodeBaseDir, baseNodeRoleDn + ".ldif");
			if (!nodeRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(baseNodeRoleDn + ".ldif"),
							nodeRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			nodeRolesUri = nodeRolesFile.toURI().toString();
		}

		Dictionary<String, ?> nodeRolesProperties = UserAdminConf.uriAsProperties(nodeRolesUri);
		if (!nodeRolesProperties.get(UserAdminConf.baseDn.property()).equals(baseNodeRoleDn)) {
			throw new CmsException("Invalid base dn for node roles");
			// TODO deal with "mounted" roles with a different baseDN
		}
		if (nodeRolesUri.startsWith("ldap")) {
			nodeRoles = new LdapUserAdmin(nodeRolesProperties);
		} else {
			nodeRoles = new LdifUserAdmin(nodeRolesProperties);
		}
		((UserDirectory) nodeRoles).setExternalRoles(this);
		((UserDirectory) nodeRoles).init();
		addUserAdmin(baseNodeRoleDn, (UserAdmin) nodeRoles);
		if (log.isTraceEnabled())
			log.trace("Node roles enabled.");

	}

	/*
	 * JCR
	 */
	private void initJcr(Session adminSession) {
		try {
			JcrUtils.mkdirs(adminSession, homeBasePath);
			JcrUtils.mkdirs(adminSession, peopleBasePath);
			adminSession.save();

			JcrUtils.addPrivilege(adminSession, homeBasePath, AuthConstants.ROLE_USER_ADMIN, Privilege.JCR_READ);
			JcrUtils.addPrivilege(adminSession, peopleBasePath, AuthConstants.ROLE_USER_ADMIN, Privilege.JCR_ALL);
			adminSession.save();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot initialize node user admin", e);
		}
	}

	private Node syncJcr(Session session, Authorization authorization) {
		// TODO check user name validity (e.g. should not start by ROLE_)
		String username = authorization.getName();
		// String[] roles = authorization.getRoles();
		try {
			Node userHome = UserJcrUtils.getUserHome(session, username);
			if (userHome == null) {
				String homePath = generateUserPath(homeBasePath, username);
				if (session.itemExists(homePath))// duplicate user id
					userHome = session.getNode(homePath).getParent().addNode(JcrUtils.lastPathElement(homePath));
				else
					userHome = JcrUtils.mkdirs(session, homePath);
				// userHome = JcrUtils.mkfolders(session, homePath);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				session.save();

				JcrUtils.clearAccessControList(session, homePath, username);
				JcrUtils.addPrivilege(session, homePath, username, Privilege.JCR_ALL);
			}

			Node userProfile = UserJcrUtils.getUserProfile(session, username);
			// new user
			if (userProfile == null) {
				String personPath = generateUserPath(peopleBasePath, username);
				Node personBase;
				if (session.itemExists(personPath))// duplicate user id
					personBase = session.getNode(personPath).getParent().addNode(JcrUtils.lastPathElement(personPath));
				else
					personBase = JcrUtils.mkdirs(session, personPath);
				userProfile = personBase.addNode(ArgeoNames.ARGEO_PROFILE);
				userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
				userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, true);
				userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED, true);
				userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED, true);
				userProfile.setProperty(ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED, true);
				session.save();

				JcrUtils.clearAccessControList(session, userProfile.getPath(), username);
				JcrUtils.addPrivilege(session, userProfile.getPath(), username, Privilege.JCR_READ);
			}

			// Remote roles
			// if (roles != null) {
			// writeRemoteRoles(userProfile, roles);
			// }
			if (adminSession.hasPendingChanges())
				adminSession.save();
			return userProfile;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot sync node security model for " + username, e);
		}
	}

	/** Generate path for a new user home */
	private String generateUserPath(String base, String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new ArgeoException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		int atIndex = userId.indexOf('@');
		if (atIndex > 0) {
			String domain = userId.substring(0, atIndex);
			String name = userId.substring(atIndex + 1);
			return base + '/' + JcrUtils.firstCharsToPath(domain, 2) + '/' + domain + '/'
					+ JcrUtils.firstCharsToPath(name, 2) + '/' + name;
		} else if (atIndex == 0 || atIndex == (userId.length() - 1)) {
			throw new ArgeoException("Unsupported username " + userId);
		} else {
			return base + '/' + JcrUtils.firstCharsToPath(userId, 2) + '/' + userId;
		}
	}

	// /** Write remote roles used by remote access in the home directory */
	// private void writeRemoteRoles(Node userHome, String[] roles)
	// throws RepositoryException {
	// boolean writeRoles = false;
	// if (userHome.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
	// Value[] remoteRoles = userHome.getProperty(
	// ArgeoNames.ARGEO_REMOTE_ROLES).getValues();
	// if (remoteRoles.length != roles.length)
	// writeRoles = true;
	// else
	// for (int i = 0; i < remoteRoles.length; i++)
	// if (!remoteRoles[i].getString().equals(roles[i]))
	// writeRoles = true;
	// } else
	// writeRoles = true;
	//
	// if (writeRoles) {
	// userHome.getSession().getWorkspace().getVersionManager()
	// .checkout(userHome.getPath());
	// userHome.setProperty(ArgeoNames.ARGEO_REMOTE_ROLES, roles);
	// JcrUtils.updateLastModified(userHome);
	// userHome.getSession().save();
	// userHome.getSession().getWorkspace().getVersionManager()
	// .checkin(userHome.getPath());
	// if (log.isDebugEnabled())
	// log.debug("Wrote remote roles " + roles + " for "
	// + userHome.getProperty(ArgeoNames.ARGEO_USER_ID));
	// }
	//
	// }
}
