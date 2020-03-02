package org.argeo.cms;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.security.auth.Subject;
import javax.transaction.UserTransaction;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Provide method interfaces to manage user concepts without accessing directly
 * the userAdmin.
 */
public interface CmsUserManager {

	// CurrentUser
	/** Returns the e-mail of the current logged in user */
	public String getMyMail();

	// Other users
	/** Returns a {@link User} given a username */
	public User getUser(String username);

	/** Can be a group or a user */
	public String getUserDisplayName(String dn);

	/** Can be a group or a user */
	public String getUserMail(String dn);

	/** Lists all roles of the given user */
	public String[] getUserRoles(String dn);

	/** Checks if the passed user belongs to the passed role */
	public boolean isUserInRole(String userDn, String roleDn);

	// Search
	/** Returns a filtered list of roles */
	public Role[] getRoles(String filter) throws InvalidSyntaxException;

	/** Recursively lists users in a given group. */
	public Set<User> listUsersInGroup(String groupDn, String filter);

	/** Search among groups including system roles and users if needed */
	public List<User> listGroups(String filter, boolean includeUsers, boolean includeSystemRoles);

	/* MISCELLANEOUS */
	/** Returns the dn of a role given its local ID */
	public String buildDefaultDN(String localId, int type);

	/** Exposes the main default domain name for this instance */
	public String getDefaultDomainName();

	/**
	 * Search for a {@link User} (might also be a group) whose uid or cn is equals
	 * to localId within the various user repositories defined in the current
	 * context.
	 */
	public User getUserFromLocalId(String localId);

	void changeOwnPassword(char[] oldPassword, char[] newPassword);

	void resetPassword(String username, char[] newPassword);

	@Deprecated
	String addSharedSecret(String username, int hours);

//	String addSharedSecret(String username, String authInfo, String authToken);

	void addAuthToken(String userDn, String token, Integer hours, String... roles);

	void addAuthToken(String userDn, String token, ZonedDateTime expiryDate, String... roles);

	void expireAuthToken(String token);

	void expireAuthTokens(Subject subject);

	User createUserFromPerson(Node person);

	@Deprecated
	public UserAdmin getUserAdmin();

	@Deprecated
	public UserTransaction getUserTransaction();
}