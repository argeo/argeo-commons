package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
public class AggregatingUserAdmin implements UserAdmin {
	private final LdapName systemRolesBaseDn;

	// DAOs
	private AbstractUserDirectory systemRoles = null;
	private Map<LdapName, AbstractUserDirectory> businessRoles = new HashMap<LdapName, AbstractUserDirectory>();

	public AggregatingUserAdmin(String systemRolesBaseDn) {
		try {
			this.systemRolesBaseDn = new LdapName(systemRolesBaseDn);
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Cannot initialize " + AggregatingUserAdmin.class, e);
		}
	}

	@Override
	public Role createRole(String name, int type) {
		return findUserAdmin(name).createRole(name, type);
	}

	@Override
	public boolean removeRole(String name) {
		boolean actuallyDeleted = findUserAdmin(name).removeRole(name);
		systemRoles.removeRole(name);
		return actuallyDeleted;
	}

	@Override
	public Role getRole(String name) {
		return findUserAdmin(name).getRole(name);
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		List<Role> res = new ArrayList<Role>();
		for (UserAdmin userAdmin : businessRoles.values()) {
			res.addAll(Arrays.asList(userAdmin.getRoles(filter)));
		}
		res.addAll(Arrays.asList(systemRoles.getRoles(filter)));
		return res.toArray(new Role[res.size()]);
	}

	@Override
	public User getUser(String key, String value) {
		List<User> res = new ArrayList<User>();
		for (UserAdmin userAdmin : businessRoles.values()) {
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
			return systemRoles.getAuthorization(null);
		}
		UserAdmin userAdmin = findUserAdmin(user.getName());
		Authorization rawAuthorization = userAdmin.getAuthorization(user);
		String usernameToUse;
		String displayNameToUse;
		if (user instanceof Group) {
			String ownerDn = TokenUtils.userDn((Group) user);
			if (ownerDn != null) {// tokens
				UserAdmin ownerUserAdmin = findUserAdmin(ownerDn);
				User ownerUser = (User) ownerUserAdmin.getRole(ownerDn);
				usernameToUse = ownerDn;
				displayNameToUse = LdifAuthorization.extractDisplayName(ownerUser);
			} else {
				usernameToUse = rawAuthorization.getName();
				displayNameToUse = rawAuthorization.toString();
			}
		} else {// regular users
			usernameToUse = rawAuthorization.getName();
			displayNameToUse = rawAuthorization.toString();
		}
		// gather system roles
		Set<String> sysRoles = new HashSet<String>();
		for (String role : rawAuthorization.getRoles()) {
			Authorization auth = systemRoles.getAuthorization((User) userAdmin.getRole(role));
			sysRoles.addAll(Arrays.asList(auth.getRoles()));
		}
		addAbstractSystemRoles(rawAuthorization, sysRoles);
		Authorization authorization = new AggregatingAuthorization(usernameToUse, displayNameToUse, sysRoles,
				rawAuthorization.getRoles());
		return authorization;
	}

	/**
	 * Enrich with application-specific roles which are strictly programmatic, such
	 * as anonymous/user semantics.
	 */
	protected void addAbstractSystemRoles(Authorization rawAuthorization, Set<String> sysRoles) {

	}

	//
	// USER ADMIN AGGREGATOR
	//
	protected void addUserDirectory(AbstractUserDirectory userDirectory) {
		LdapName baseDn = userDirectory.getBaseDn();
		if (isSystemRolesBaseDn(baseDn)) {
			this.systemRoles = userDirectory;
			systemRoles.setExternalRoles(this);
		} else {
			if (businessRoles.containsKey(baseDn))
				throw new UserDirectoryException("There is already a user admin for " + baseDn);
			businessRoles.put(baseDn, userDirectory);
		}
		userDirectory.init();
		postAdd(userDirectory);
	}

	/** Called after a new user directory has been added */
	protected void postAdd(AbstractUserDirectory userDirectory) {
	}

	private UserAdmin findUserAdmin(String name) {
		try {
			UserAdmin userAdmin = findUserAdmin(new LdapName(name));
			return userAdmin;
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted name " + name, e);
		}
	}

	private UserAdmin findUserAdmin(LdapName name) {
		if (name.startsWith(systemRolesBaseDn))
			return systemRoles;
		List<UserAdmin> res = new ArrayList<UserAdmin>(1);
		for (LdapName baseDn : businessRoles.keySet()) {
			if (name.startsWith(baseDn)) {
				AbstractUserDirectory ud = businessRoles.get(baseDn);
				if (!ud.isDisabled())
					res.add(ud);
			}
		}
		if (res.size() == 0)
			throw new UserDirectoryException("Cannot find user admin for " + name);
		if (res.size() > 1)
			throw new UserDirectoryException("Multiple user admin found for " + name);
		return res.get(0);
	}

	protected boolean isSystemRolesBaseDn(LdapName baseDn) {
		return baseDn.equals(systemRolesBaseDn);
	}

	protected Dictionary<String, Object> currentState() {
		Dictionary<String, Object> res = new Hashtable<String, Object>();
		// res.put(NodeConstants.CN, NodeConstants.DEFAULT);
		for (LdapName name : businessRoles.keySet()) {
			AbstractUserDirectory userDirectory = businessRoles.get(name);
			String uri = UserAdminConf.propertiesAsUri(userDirectory.getProperties()).toString();
			res.put(uri, "");
		}
		return res;
	}

	public void destroy() {
		for (LdapName name : businessRoles.keySet()) {
			AbstractUserDirectory userDirectory = businessRoles.get(name);
			destroy(userDirectory);
		}
		businessRoles.clear();
		businessRoles = null;
		destroy(systemRoles);
		systemRoles = null;
	}

	private void destroy(AbstractUserDirectory userDirectory) {
		preDestroy(userDirectory);
		userDirectory.destroy();
	}

	protected void removeUserDirectory(LdapName baseDn) {
		if (isSystemRolesBaseDn(baseDn))
			throw new UserDirectoryException("System roles cannot be removed ");
		if (!businessRoles.containsKey(baseDn))
			throw new UserDirectoryException("No user directory registered for " + baseDn);
		AbstractUserDirectory userDirectory = businessRoles.remove(baseDn);
		destroy(userDirectory);
	}

	/**
	 * Called before each user directory is destroyed, so that additional actions
	 * can be performed.
	 */
	protected void preDestroy(AbstractUserDirectory userDirectory) {
	}

}
