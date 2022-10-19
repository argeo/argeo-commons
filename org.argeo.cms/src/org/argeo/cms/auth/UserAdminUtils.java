package org.argeo.cms.auth;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.api.cms.CmsConstants;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Centralise common patterns to manage users with a {@link UserAdmin} */
public class UserAdminUtils {

	// CURRENTUSER HELPERS
	/** Checks if current user is the same as the passed one */
	public static boolean isCurrentUser(User user) {
		String userUsername = getProperty(user, LdapAttrs.DN);
		LdapName userLdapName = getLdapName(userUsername);
		LdapName selfUserName = getCurrentUserLdapName();
		return userLdapName.equals(selfUserName);
	}

	/** Retrieves the current logged-in {@link User} */
	public static User getCurrentUser(UserAdmin userAdmin) {
		return (User) userAdmin.getRole(CurrentUser.getUsername());
	}

	/** Retrieves the current logged-in user {@link LdapName} */
	public final static LdapName getCurrentUserLdapName() {
		String name = CurrentUser.getUsername();
		return getLdapName(name);
	}

	/** Retrieves the current logged-in user mail */
	public static String getCurrentUserMail(UserAdmin userAdmin) {
		String username = CurrentUser.getUsername();
		return getUserMail(userAdmin, username);
	}

	/** Retrieves the current logged-in user common name */
	public final static String getCommonName(User user) {
		return getProperty(user, LdapAttrs.cn.name());
	}

	// OTHER USERS HELPERS
	/**
	 * Retrieves the local id of a user or group, that is respectively the uid or cn
	 * of the passed dn with no {@link UserAdmin}
	 */
	public static String getUserLocalId(String dn) {
		LdapName ldapName = getLdapName(dn);
		Rdn last = ldapName.getRdn(ldapName.size() - 1);
		if (last.getType().toLowerCase().equals(LdapAttrs.uid.name())
				|| last.getType().toLowerCase().equals(LdapAttrs.cn.name()))
			return (String) last.getValue();
		else
			throw new IllegalArgumentException("Cannot retrieve user local id, non valid dn: " + dn);
	}

	/**
	 * Returns the local username if no user with this dn is found or if the found
	 * user has no defined display name
	 */
	public static String getUserDisplayName(UserAdmin userAdmin, String dn) {
		Role user = userAdmin.getRole(dn);
		if (user == null)
			return getUserLocalId(dn);
		return getUserDisplayName(user);
	}

	public static String getUserDisplayName(Role user) {
		String dName = getProperty(user, LdapAttrs.displayName.name());
		if (isEmpty(dName))
			dName = getProperty(user, LdapAttrs.cn.name());
		if (isEmpty(dName))
			dName = getProperty(user, LdapAttrs.uid.name());
		if (isEmpty(dName))
			dName = getUserLocalId(user.getName());
		return dName;
	}

	/**
	 * Returns null if no user with this dn is found or if the found user has no
	 * defined mail
	 */
	public static String getUserMail(UserAdmin userAdmin, String dn) {
		Role user = userAdmin.getRole(dn);
		if (user == null)
			return null;
		else
			return getProperty(user, LdapAttrs.mail.name());
	}

	// LDAP NAMES HELPERS
	/**
	 * Easily retrieves one of the {@link Role}'s property or an empty String if the
	 * requested property is not defined
	 */
	public final static String getProperty(Role role, String key) {
		Object obj = role.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	public final static String getProperty(Role role, Enum<?> key) {
		Object obj = role.getProperties().get(key.name());
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	public final static void setProperty(Role role, String key, String value) {
		role.getProperties().put(key, value);
	}

	public final static void setProperty(Role role, Enum<?> key, String value) {
		setProperty(role, key.name(), value);
	}

	/**
	 * Simply retrieves a LDAP name from a {@link LdapAttrs.DN} with no exception
	 */
	private static LdapName getLdapName(String dn) {
		try {
			return new LdapName(dn);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot parse LDAP name " + dn, e);
		}
	}

	/** Simply retrieves a display name of the relevant domain */
	public final static String getDomainName(User user) {
		String dn = user.getName();
		if (dn.endsWith(CmsConstants.SYSTEM_ROLES_BASEDN))
			return "System roles";
		if (dn.endsWith(CmsConstants.TOKENS_BASEDN))
			return "Tokens";
		try {
			// FIXME deal with non-DC
			LdapName name = new LdapName(dn);
			List<Rdn> rdns = name.getRdns();
			String dname = null;
			int i = 0;
			loop: while (i < rdns.size()) {
				Rdn currrRdn = rdns.get(i);
				if (LdapAttrs.uid.name().equals(currrRdn.getType()) || LdapAttrs.cn.name().equals(currrRdn.getType())
						|| LdapAttrs.ou.name().equals(currrRdn.getType()))
					break loop;
				else {
					String currVal = (String) currrRdn.getValue();
					dname = dname == null ? currVal : currVal + "." + dname;
				}
				i++;
			}
			return dname;
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Unable to get domain name for " + dn, e);
		}
	}

	// VARIOUS HELPERS
	public final static String buildDefaultCn(String firstName, String lastName) {
		return (firstName.trim() + " " + lastName.trim() + " ").trim();
	}

	/** Simply checks if a string is null or empty */
	private static boolean isEmpty(String stringToTest) {
		return stringToTest == null || "".equals(stringToTest.trim());
	}

}
