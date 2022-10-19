package org.argeo.cms;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.argeo.cms.auth.SystemRole;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.directory.HierarchyUnit;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Provide method interfaces to manage user concepts without accessing directly
 * the userAdmin.
 */
public interface CmsUserManager {
	Map<String, String> getKnownBaseDns(boolean onlyWritable);

	Set<UserDirectory> getUserDirectories();

	// CurrentUser
	/** Returns the e-mail of the current logged in user */
	String getMyMail();

	// Other users
	/** Returns a {@link User} given a username */
	User getUser(String username);

	/** Can be a group or a user */
	String getUserDisplayName(String dn);

	/** Can be a group or a user */
	String getUserMail(String dn);

	/** Lists all roles of the given user */
	String[] getUserRoles(String dn);

	/** Checks if the passed user belongs to the passed role */
	boolean isUserInRole(String userDn, String roleDn);

	// Search
	/** Returns a filtered list of roles */
	Role[] getRoles(String filter) throws InvalidSyntaxException;

	/** Recursively lists users in a given group. */
	Set<User> listUsersInGroup(String groupDn, String filter);

	/** Search among groups including system roles and users if needed */
	List<User> listGroups(String filter, boolean includeUsers, boolean includeSystemRoles);

//	/**
//	 * Lists functional accounts, that is users with regular access to the system
//	 * under this functional hierarchy unit (which probably have technical direct
//	 * sub hierarchy units), excluding groups which are not explicitly users.
//	 */
//	Set<User> listAccounts(HierarchyUnit hierarchyUnit, boolean deep);

	/*
	 * EDITION
	 */
	/** Creates a new user. */
	User createUser(String username, Map<String, Object> properties, Map<String, Object> credentials);

	/** Creates a group. */
	Group getOrCreateGroup(HierarchyUnit groups, String commonName);

	/** Creates a new system role. */
	Group getOrCreateSystemRole(HierarchyUnit roles, SystemRole systemRole);

	/** Add additional object classes to this role. */
	void addObjectClasses(Role role, Set<String> objectClasses, Map<String, Object> additionalProperties);

	/** Add additional object classes to this hierarchy unit. */
	void addObjectClasses(HierarchyUnit hierarchyUnit, Set<String> objectClasses,
			Map<String, Object> additionalProperties);

	/** Add a member to this group. */
	void addMember(Group group, Role role);
	
	void edit(Runnable action);

	/* MISCELLANEOUS */
	/** Returns the dn of a role given its local ID */
	String buildDefaultDN(String localId, int type);

	/** Exposes the main default domain name for this instance */
	String getDefaultDomainName();

	/**
	 * Search for a {@link User} (might also be a group) whose uid or cn is equals
	 * to localId within the various user repositories defined in the current
	 * context.
	 */
	User getUserFromLocalId(String localId);

	void changeOwnPassword(char[] oldPassword, char[] newPassword);

	void resetPassword(String username, char[] newPassword);

	@Deprecated
	String addSharedSecret(String username, int hours);

//	String addSharedSecret(String username, String authInfo, String authToken);

	void addAuthToken(String userDn, String token, Integer hours, String... roles);

	void addAuthToken(String userDn, String token, ZonedDateTime expiryDate, String... roles);

	void expireAuthToken(String token);

	void expireAuthTokens(Subject subject);

	UserDirectory getDirectory(Role role);

	/** Create a new hierarchy unit. Does nothing if it already exists. */
	HierarchyUnit getOrCreateHierarchyUnit(UserDirectory directory, String path);
}