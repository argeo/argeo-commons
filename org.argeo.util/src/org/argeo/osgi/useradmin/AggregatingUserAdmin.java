package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.AbstractUserDirectory.toLdapName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	private final LdapName tokensBaseDn;

	// DAOs
	private AbstractUserDirectory systemRoles = null;
	private AbstractUserDirectory tokens = null;
	private Map<LdapName, AbstractUserDirectory> businessRoles = new HashMap<LdapName, AbstractUserDirectory>();

	// TODO rather use an empty constructor and an init method
	public AggregatingUserAdmin(String systemRolesBaseDn, String tokensBaseDn) {
		try {
			this.systemRolesBaseDn = new LdapName(systemRolesBaseDn);
			if (tokensBaseDn != null)
				this.tokensBaseDn = new LdapName(tokensBaseDn);
			else
				this.tokensBaseDn = null;
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
		AbstractUserDirectory userReferentialOfThisUser = findUserAdmin(user.getName());
		Authorization rawAuthorization = userReferentialOfThisUser.getAuthorization(user);
		String usernameToUse;
		String displayNameToUse;
		if (user instanceof Group) {
			// TODO check whether this is still working
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

		// gather roles from other referentials
		final AbstractUserDirectory userAdminToUse;// possibly scoped when authenticating
		if (user instanceof DirectoryUser) {
			userAdminToUse = userReferentialOfThisUser;
		} else if (user instanceof AuthenticatingUser) {
			userAdminToUse = userReferentialOfThisUser.scope(user);
		} else {
			throw new IllegalArgumentException("Unsupported user type " + user.getClass());
		}

		try {
			Set<String> sysRoles = new HashSet<String>();
			for (String role : rawAuthorization.getRoles()) {
				User userOrGroup = (User) userAdminToUse.getRole(role);
				Authorization auth = systemRoles.getAuthorization(userOrGroup);
				systemRoles: for (String systemRole : auth.getRoles()) {
					if (role.equals(systemRole))
						continue systemRoles;
					sysRoles.add(systemRole);
				}
//			sysRoles.addAll(Arrays.asList(auth.getRoles()));
			}
			addAbstractSystemRoles(rawAuthorization, sysRoles);
			Authorization authorization = new AggregatingAuthorization(usernameToUse, displayNameToUse, sysRoles,
					rawAuthorization.getRoles());
			return authorization;
		} finally {
			if (userAdminToUse != null && userAdminToUse.isScoped()) {
				userAdminToUse.destroy();
			}
		}
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
	protected void addUserDirectory(UserDirectory ud) {
		if (!(ud instanceof AbstractUserDirectory))
			throw new IllegalArgumentException("Only " + AbstractUserDirectory.class.getName() + " is supported");
		AbstractUserDirectory userDirectory = (AbstractUserDirectory) ud;
		String basePath = userDirectory.getGlobalId();
		if (isSystemRolesBaseDn(basePath)) {
			this.systemRoles = userDirectory;
			systemRoles.setExternalRoles(this);
		} else if (isTokensBaseDn(basePath)) {
			this.tokens = userDirectory;
			tokens.setExternalRoles(this);
		} else {
			LdapName baseDn = toLdapName(basePath);
			if (businessRoles.containsKey(baseDn))
				throw new UserDirectoryException("There is already a user admin for " + baseDn);
			businessRoles.put(baseDn, userDirectory);
		}
		userDirectory.init();
		postAdd(userDirectory);
	}

	/** Called after a new user directory has been added */
	protected void postAdd(UserDirectory userDirectory) {
	}

	private AbstractUserDirectory findUserAdmin(String name) {
		try {
			return findUserAdmin(new LdapName(name));
		} catch (InvalidNameException e) {
			throw new UserDirectoryException("Badly formatted name " + name, e);
		}
	}

	private AbstractUserDirectory findUserAdmin(LdapName name) {
		if (name.startsWith(systemRolesBaseDn))
			return systemRoles;
		if (tokensBaseDn != null && name.startsWith(tokensBaseDn))
			return tokens;
		List<AbstractUserDirectory> res = new ArrayList<>(1);
		userDirectories: for (LdapName baseDn : businessRoles.keySet()) {
			AbstractUserDirectory userDirectory = businessRoles.get(baseDn);
			if (name.startsWith(baseDn)) {
				if (userDirectory.isDisabled())
					continue userDirectories;
//				if (res.isEmpty()) {
				res.add(userDirectory);
//				} else {
//					for (AbstractUserDirectory ud : res) {
//						LdapName bd = ud.getBaseDn();
//						if (userDirectory.getBaseDn().startsWith(bd)) {
//							// child user directory
//						}
//					}
//				}
			}
		}
		if (res.size() == 0)
			throw new UserDirectoryException("Cannot find user admin for " + name);
		if (res.size() > 1)
			throw new UserDirectoryException("Multiple user admin found for " + name);
		return res.get(0);
	}

	protected boolean isSystemRolesBaseDn(String basePath) {
		return toLdapName(basePath).equals(systemRolesBaseDn);
	}

	protected boolean isTokensBaseDn(String basePath) {
		return tokensBaseDn != null && toLdapName(basePath).equals(tokensBaseDn);
	}

//	protected Dictionary<String, Object> currentState() {
//		Dictionary<String, Object> res = new Hashtable<String, Object>();
//		// res.put(NodeConstants.CN, NodeConstants.DEFAULT);
//		for (LdapName name : businessRoles.keySet()) {
//			AbstractUserDirectory userDirectory = businessRoles.get(name);
//			String uri = UserAdminConf.propertiesAsUri(userDirectory.getProperties()).toString();
//			res.put(uri, "");
//		}
//		return res;
//	}

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

	protected void removeUserDirectory(String basePath) {
		if (isSystemRolesBaseDn(basePath))
			throw new UserDirectoryException("System roles cannot be removed ");
		LdapName baseDn = toLdapName(basePath);
		if (!businessRoles.containsKey(baseDn))
			throw new UserDirectoryException("No user directory registered for " + baseDn);
		AbstractUserDirectory userDirectory = businessRoles.remove(baseDn);
		destroy(userDirectory);
	}

	/**
	 * Called before each user directory is destroyed, so that additional actions
	 * can be performed.
	 */
	protected void preDestroy(UserDirectory userDirectory) {
	}

	public Set<UserDirectory> getUserDirectories() {
		TreeSet<UserDirectory> res = new TreeSet<>((o1, o2) -> o1.getGlobalId().compareTo(o2.getGlobalId()));
		res.addAll(businessRoles.values());
		return res;
	}
}
