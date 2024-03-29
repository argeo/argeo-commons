package org.argeo.cms.osgi.useradmin;

import static org.argeo.cms.osgi.useradmin.DirectoryUserAdmin.toLdapName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.directory.CmsUser;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.cms.runtime.DirectoryConf;
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
	private DirectoryUserAdmin systemRoles = null;
	private DirectoryUserAdmin tokens = null;
	private Map<LdapName, DirectoryUserAdmin> businessRoles = new HashMap<LdapName, DirectoryUserAdmin>();

	// TODO rather use an empty constructor and an init method
	public AggregatingUserAdmin(String systemRolesBaseDn, String tokensBaseDn) {
		try {
			this.systemRolesBaseDn = new LdapName(systemRolesBaseDn);
			if (tokensBaseDn != null)
				this.tokensBaseDn = new LdapName(tokensBaseDn);
			else
				this.tokensBaseDn = null;
		} catch (InvalidNameException e) {
			throw new IllegalStateException("Cannot initialize " + AggregatingUserAdmin.class, e);
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

	/** Builds an authorisation by scanning all referentials. */
	@Override
	public Authorization getAuthorization(User user) {
		if (user == null) {// anonymous
			return systemRoles.getAuthorization(null);
		}
		DirectoryUserAdmin userReferentialOfThisUser = findUserAdmin(user.getName());
		Authorization rawAuthorization = userReferentialOfThisUser.getAuthorization(user);
		User retrievedUser = (User) userReferentialOfThisUser.getRole(user.getName());
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
		List<String> rawRoles = Arrays.asList(rawAuthorization.getRoles());
		List<String> allRoles = new ArrayList<>(rawRoles);
		for (LdapName otherBaseDn : businessRoles.keySet()) {
			if (otherBaseDn.equals(userReferentialOfThisUser.getBaseDn()))
				continue;
			DirectoryUserAdmin otherUserAdmin = userAdminToUse(user, businessRoles.get(otherBaseDn));
			if (otherUserAdmin == null)
				continue;
			for (String roleStr : rawRoles) {
				User role = (User) findUserAdmin(roleStr).getRole(roleStr);
				Authorization auth = otherUserAdmin.getAuthorization(role);
				allRoles.addAll(Arrays.asList(auth.getRoles()));
			}

		}

		// integrate system roles
		final DirectoryUserAdmin userAdminToUse = userAdminToUse(retrievedUser, userReferentialOfThisUser);
		Objects.requireNonNull(userAdminToUse);

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
					allRoles.toArray(new String[allRoles.size()]));
			return authorization;
		} finally {
			if (userAdminToUse != null && userAdminToUse.isScoped()) {
				userAdminToUse.destroy();
			}
		}
	}

	/** Decide whether to scope or not */
	private DirectoryUserAdmin userAdminToUse(User user, DirectoryUserAdmin userAdmin) {
		if (userAdmin.isAuthenticated())
			return userAdmin;
		if (user instanceof CmsUser) {
			return userAdmin;
		} else if (user instanceof AuthenticatingUser) {
			return userAdmin.scope(user).orElse(null);
		} else {
			throw new IllegalArgumentException("Unsupported user type " + user.getClass());
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
		if (!(ud instanceof DirectoryUserAdmin))
			throw new IllegalArgumentException("Only " + DirectoryUserAdmin.class.getName() + " is supported");
		DirectoryUserAdmin userDirectory = (DirectoryUserAdmin) ud;
		String basePath = userDirectory.getBase();
		if (isSystemRolesBaseDn(basePath)) {
			this.systemRoles = userDirectory;
			systemRoles.setExternalRoles(this);
		} else if (isTokensBaseDn(basePath)) {
			this.tokens = userDirectory;
			tokens.setExternalRoles(this);
		} else {
			LdapName baseDn = toLdapName(basePath);
			if (businessRoles.containsKey(baseDn))
				throw new IllegalStateException("There is already a user admin for " + baseDn);
			businessRoles.put(baseDn, userDirectory);
		}
		userDirectory.init();
		postAdd(userDirectory);
	}

	/** Called after a new user directory has been added */
	protected void postAdd(UserDirectory userDirectory) {
	}

	private DirectoryUserAdmin findUserAdmin(String name) {
		try {
			return findUserAdmin(new LdapName(name));
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Badly formatted name " + name, e);
		}
	}

	private DirectoryUserAdmin findUserAdmin(LdapName name) {
		if (name.startsWith(systemRolesBaseDn))
			return systemRoles;
		if (tokensBaseDn != null && name.startsWith(tokensBaseDn))
			return tokens;
		List<DirectoryUserAdmin> res = new ArrayList<>(1);
		userDirectories: for (LdapName baseDn : businessRoles.keySet()) {
			DirectoryUserAdmin userDirectory = businessRoles.get(baseDn);
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
			throw new IllegalStateException("Cannot find user admin for " + name);
		if (res.size() > 1)
			throw new IllegalStateException("Multiple user admin found for " + name);
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

	public void start() {
		if (systemRoles == null) {
			// TODO do we really need separate system roles?
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(DirectoryConf.baseDn.name(), "ou=roles,ou=system");
			systemRoles = new DirectoryUserAdmin(properties);
		}
	}

	public void stop() {
		for (LdapName name : businessRoles.keySet()) {
			DirectoryUserAdmin userDirectory = businessRoles.get(name);
			destroy(userDirectory);
		}
		businessRoles.clear();
		businessRoles = null;
		destroy(systemRoles);
		systemRoles = null;
	}

	private void destroy(DirectoryUserAdmin userDirectory) {
		preDestroy(userDirectory);
		userDirectory.destroy();
	}

//	protected void removeUserDirectory(UserDirectory userDirectory) {
//		LdapName baseDn = toLdapName(userDirectory.getContext());
//		businessRoles.remove(baseDn);
//		if (userDirectory instanceof DirectoryUserAdmin)
//			destroy((DirectoryUserAdmin) userDirectory);
//	}

	@Deprecated
	protected void removeUserDirectory(String basePath) {
		if (isSystemRolesBaseDn(basePath))
			throw new IllegalArgumentException("System roles cannot be removed ");
		LdapName baseDn = toLdapName(basePath);
		if (!businessRoles.containsKey(baseDn))
			throw new IllegalStateException("No user directory registered for " + baseDn);
		DirectoryUserAdmin userDirectory = businessRoles.remove(baseDn);
		destroy(userDirectory);
	}

	/**
	 * Called before each user directory is destroyed, so that additional actions
	 * can be performed.
	 */
	protected void preDestroy(UserDirectory userDirectory) {
	}

	public Set<UserDirectory> getUserDirectories() {
		TreeSet<UserDirectory> res = new TreeSet<>((o1, o2) -> o1.getBase().compareTo(o2.getBase()));
		res.addAll(businessRoles.values());
		res.add(systemRoles);
		return res;
	}

}
