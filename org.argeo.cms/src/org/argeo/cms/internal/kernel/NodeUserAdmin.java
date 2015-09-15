package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.KernelHeader;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.LdapProperties;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.UserDirectoryException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class NodeUserAdmin implements UserAdmin {
	private final static Log log = LogFactory.getLog(NodeUserAdmin.class);
	final static LdapName ROLES_BASE;
	static {
		try {
			ROLES_BASE = new LdapName(KernelHeader.ROLES_BASEDN);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Cannot initialize "
					+ NodeUserAdmin.class, e);
		}
	}

	private UserAdmin nodeRoles = null;
	private Map<LdapName, UserAdmin> userAdmins = new HashMap<LdapName, UserAdmin>();

	public NodeUserAdmin() {
		File osgiInstanceDir = KernelUtils.getOsgiInstanceDir();
		File nodeBaseDir = new File(osgiInstanceDir, "node");
		nodeBaseDir.mkdirs();

		String userAdminUri = KernelUtils
				.getFrameworkProp(KernelConstants.USERADMIN_URI);
		if (userAdminUri == null) {
			String demoBaseDn = "dc=example,dc=com";
			File businessRolesFile = new File(nodeBaseDir, demoBaseDn + ".ldif");
			if (!businessRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass()
							.getResourceAsStream(demoBaseDn + ".ldif"),
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
				if (u.getScheme() == null) {
					if (uri.startsWith("/"))
						u = new File(uri).getAbsoluteFile().toURI();
					else if (!uri.contains("/"))
						u = new File(nodeBaseDir, uri).getAbsoluteFile()
								.toURI();
					else
						throw new CmsException("Cannot interpret " + uri
								+ " as an uri");
				}
			} catch (URISyntaxException e) {
				throw new CmsException(
						"Cannot interpret " + uri + " as an uri", e);
			}
			Dictionary<String, ?> properties = LdapProperties.uriAsProperties(u
					.toString());
			AbstractUserDirectory businessRoles;
			if (u.getScheme().startsWith("ldap")) {
				businessRoles = new LdapUserAdmin(properties);
			} else {
				businessRoles = new LdifUserAdmin(properties);
			}
			businessRoles.init();
			addUserAdmin(businessRoles.getBaseDn(), businessRoles);
			if (log.isDebugEnabled())
				log.debug("User directory " + businessRoles.getBaseDn() + " ["
						+ u.getScheme() + "] enabled.");
		}

		// NOde roles
		String nodeRolesUri = KernelUtils
				.getFrameworkProp(KernelConstants.ROLES_URI);
		String baseNodeRoleDn = KernelHeader.ROLES_BASEDN;
		if (nodeRolesUri == null) {
			File nodeRolesFile = new File(nodeBaseDir, baseNodeRoleDn + ".ldif");
			if (!nodeRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass()
							.getResourceAsStream("demo.ldif"), nodeRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			nodeRolesUri = nodeRolesFile.toURI().toString();
		}

		Dictionary<String, ?> nodeRolesProperties = LdapProperties
				.uriAsProperties(nodeRolesUri);
		if (!nodeRolesProperties.get(LdapProperties.baseDn.getFullName())
				.equals(baseNodeRoleDn)) {
			throw new CmsException("Invalid base dn for node roles");
			// TODO deal with "mounted" roles with a different baseDN
		}
		AbstractUserDirectory nodeRoles;
		if (nodeRolesUri.startsWith("ldap")) {
			nodeRoles = new LdapUserAdmin(nodeRolesProperties);
		} else {
			nodeRoles = new LdifUserAdmin(nodeRolesProperties);
		}
		nodeRoles.setExternalRoles(this);
		nodeRoles.init();
		addUserAdmin(baseNodeRoleDn, nodeRoles);
		if (log.isTraceEnabled())
			log.trace("Node roles enabled.");
	}

	String asConfigUris() {
		StringBuilder buf = new StringBuilder();
		for (LdapName name : userAdmins.keySet()) {
			buf.append('/').append(name.toString());
			if (userAdmins.get(name) instanceof AbstractUserDirectory) {
				AbstractUserDirectory userDirectory = (AbstractUserDirectory) userAdmins
						.get(name);
				if (userDirectory.isReadOnly())
					buf.append('?').append(LdapProperties.readOnly.name())
							.append("=true");
			}
			buf.append(' ');
		}
		return buf.toString();
	}

	public void destroy() {
		for (LdapName name : userAdmins.keySet()) {
			if (userAdmins.get(name) instanceof AbstractUserDirectory) {
				AbstractUserDirectory userDirectory = (AbstractUserDirectory) userAdmins
						.get(name);
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
		return findUserAdmin(name).removeRole(name);
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
		if (user == null) {
			return nodeRoles.getAuthorization(null);
		}
		UserAdmin userAdmin = findUserAdmin(user.getName());
		Authorization rawAuthorization = userAdmin.getAuthorization(user);
		// gather system roles
		Set<String> systemRoles = new HashSet<String>();
		for (String role : rawAuthorization.getRoles()) {
			Authorization auth = nodeRoles.getAuthorization((User) userAdmin
					.getRole(role));
			systemRoles.addAll(Arrays.asList(auth.getRoles()));
		}
		return new NodeAuthorization(rawAuthorization.getName(),
				rawAuthorization.toString(), systemRoles,
				rawAuthorization.getRoles());
	}

	//
	// USER ADMIN AGGREGATOR
	//
	public synchronized void addUserAdmin(String baseDn, UserAdmin userAdmin) {
		if (baseDn.equals(KernelHeader.ROLES_BASEDN)) {
			nodeRoles = userAdmin;
			return;
		}

		if (userAdmins.containsKey(baseDn))
			throw new UserDirectoryException(
					"There is already a user admin for " + baseDn);
		try {
			userAdmins.put(new LdapName(baseDn), userAdmin);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted base DN "
					+ baseDn, e);
		}
	}

	public synchronized void removeUserAdmin(String baseDn) {
		if (baseDn.equals(KernelHeader.ROLES_BASEDN))
			throw new UserDirectoryException("Node roles cannot be removed.");
		LdapName base;
		try {
			base = new LdapName(baseDn);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted base DN "
					+ baseDn, e);
		}
		if (!userAdmins.containsKey(base))
			throw new UserDirectoryException("There is no user admin for "
					+ base);
		userAdmins.remove(base);
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
			throw new UserDirectoryException("Cannot find user admin for "
					+ name);
		if (res.size() > 1)
			throw new UserDirectoryException("Multiple user admin found for "
					+ name);
		return res.get(0);
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		if (nodeRoles instanceof AbstractUserDirectory)
			((AbstractUserDirectory) nodeRoles)
					.setTransactionManager(transactionManager);
		for (UserAdmin userAdmin : userAdmins.values()) {
			if (userAdmin instanceof AbstractUserDirectory)
				((AbstractUserDirectory) userAdmin)
						.setTransactionManager(transactionManager);
		}
	}
}
