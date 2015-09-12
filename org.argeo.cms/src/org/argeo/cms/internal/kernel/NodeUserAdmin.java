package org.argeo.cms.internal.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.argeo.cms.KernelHeader;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.UserAdminAggregator;
import org.argeo.osgi.useradmin.UserDirectoryException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class NodeUserAdmin implements UserAdmin, UserAdminAggregator {
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

	private TransactionSynchronizationRegistry syncRegistry;
	private TransactionManager transactionManager;

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
	@Override
	public synchronized void addUserAdmin(String baseDn, UserAdmin userAdmin) {
		if (userAdmin instanceof AbstractUserDirectory)
			((AbstractUserDirectory) userAdmin).setSyncRegistry(syncRegistry);

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

	@Override
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
		UserAdmin userAdmin = userAdmins.remove(base);
		if (userAdmin instanceof AbstractUserDirectory)
			((AbstractUserDirectory) userAdmin).setSyncRegistry(null);
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
		this.transactionManager = transactionManager;
		if (nodeRoles instanceof AbstractUserDirectory)
			((AbstractUserDirectory) nodeRoles)
					.setTransactionManager(transactionManager);
		for (UserAdmin userAdmin : userAdmins.values()) {
			if (userAdmin instanceof AbstractUserDirectory)
				((AbstractUserDirectory) userAdmin)
						.setTransactionManager(transactionManager);
		}
	}

	public void setSyncRegistry(TransactionSynchronizationRegistry syncRegistry) {
		this.syncRegistry = syncRegistry;
		if (nodeRoles instanceof AbstractUserDirectory)
			((AbstractUserDirectory) nodeRoles).setSyncRegistry(syncRegistry);
		for (UserAdmin userAdmin : userAdmins.values()) {
			if (userAdmin instanceof AbstractUserDirectory)
				((AbstractUserDirectory) userAdmin)
						.setSyncRegistry(syncRegistry);
		}
	}

}
