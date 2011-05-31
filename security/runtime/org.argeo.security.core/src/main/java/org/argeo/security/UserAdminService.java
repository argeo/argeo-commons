package org.argeo.security;

import java.util.Set;

import org.springframework.security.userdetails.UserDetailsManager;

public interface UserAdminService extends UserDetailsManager {
	/**
	 * Usernames must match this regexp pattern ({@value #USERNAME_PATTERN}).
	 * Thanks to <a href=
	 * "http://www.mkyong.com/regular-expressions/how-to-validate-username-with-regular-expression/"
	 * >this tip</a> (modified to remove '-' and add upper-case)
	 */
	public final static String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,15}$";

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
	/** List all users. */
	public Set<String> listUsers();

	/** List users having this role (except the super user). */
	public Set<String> listUsersInRole(String role);

	/** Synchronize with the underlying DAO. */
	public void synchronize();

	/*
	 * ROLES
	 */
	public void newRole(String role);

	public Set<String> listEditableRoles();

	public void deleteRole(String role);
}
