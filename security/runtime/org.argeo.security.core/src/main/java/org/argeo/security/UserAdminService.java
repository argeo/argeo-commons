package org.argeo.security;

import java.util.List;
import java.util.Set;

public interface UserAdminService {
	/**
	 * Usernames must match this regexp pattern ({@value #USERNAME_PATTERN}).
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-username-with-regular-expression/"
	 * >this tip</a> (modified to remove '-')
	 */
	public final static String USERNAME_PATTERN = "^[a-z0-9_]{3,15}$";

	/**
	 * Email addresses must match this regexp pattern ({@value #EMAIL_PATTERN}.
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-email-address-with-regular-expression/"
	 * >this tip</a>.
	 */
	public final static String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	/*
	 * USERS
	 */
	public void newUser(ArgeoUser argeoUser);

	public ArgeoUser getUser(String username);

	public Set<ArgeoUser> listUsers();

	public Boolean userExists(String username);

	public void updateUser(ArgeoUser user);

	public void updateUserPassword(String username, String password);

	/** List users having this role (except the super user). */
	public Set<ArgeoUser> listUsersInRole(String role);

	public List<String> listUserRoles(String username);

	public void deleteUser(String username);

	/** Synchronize with the underlying DAO. */
	public void synchronize();

	/*
	 * ROLES
	 */
	public void newRole(String role);

	public Set<String> listEditableRoles();

	public void deleteRole(String role);
}
