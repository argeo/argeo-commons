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

import org.argeo.cms.KernelHeader;
import org.argeo.osgi.useradmin.ArgeoUserAdminException;
import org.argeo.osgi.useradmin.UserAdminAggregator;
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
			throw new ArgeoUserAdminException("Cannot initialize "
					+ NodeUserAdmin.class, e);
		}
	}

	private UserAdmin nodeRoles = null;
	private Map<LdapName, UserAdmin> userAdmins = new HashMap<LdapName, UserAdmin>();

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
		// FIXME clarify assumptions
		return userAdmin.getAuthorization(user);
		// String[] roles = auth.getRoles();
		// // Gather system roles
		// Set<String> systemRoles = new HashSet<String>();
		// for(String businessRole:roles){
		//
		// }
		// return null;
	}

	//
	// USER ADMIN AGGREGATOR
	//
	@Override
	public synchronized void addUserAdmin(String baseDn, UserAdmin userAdmin) {
		if (baseDn.equals(KernelHeader.ROLES_BASEDN)) {
			nodeRoles = userAdmin;
			return;
		}

		if (userAdmins.containsKey(baseDn))
			throw new ArgeoUserAdminException(
					"There is already a user admin for " + baseDn);
		try {
			userAdmins.put(new LdapName(baseDn), userAdmin);
		} catch (InvalidNameException e) {
			throw new ArgeoUserAdminException("Badly formatted base DN "
					+ baseDn, e);
		}
	}

	@Override
	public synchronized void removeUserAdmin(String baseDn) {
		if (baseDn.equals(KernelHeader.ROLES_BASEDN))
			throw new ArgeoUserAdminException("Node roles cannot be removed.");
		LdapName base;
		try {
			base = new LdapName(baseDn);
		} catch (InvalidNameException e) {
			throw new ArgeoUserAdminException("Badly formatted base DN "
					+ baseDn, e);
		}
		if (!userAdmins.containsKey(base))
			throw new ArgeoUserAdminException("There is no user admin for "
					+ base);
		userAdmins.remove(base);
	}

	private UserAdmin findUserAdmin(String name) {
		try {
			return findUserAdmin(new LdapName(name));
		} catch (InvalidNameException e) {
			throw new ArgeoUserAdminException("Badly formatted name " + name, e);
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
			throw new ArgeoUserAdminException("Cannot find user admin for "
					+ name);
		if (res.size() > 1)
			throw new ArgeoUserAdminException("Multiple user admin found for "
					+ name);
		return res.get(0);
	}
}
