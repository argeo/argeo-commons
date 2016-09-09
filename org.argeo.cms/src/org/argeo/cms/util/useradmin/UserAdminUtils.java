package org.argeo.cms.util.useradmin;

import java.security.AccessController;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsView;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.osgi.useradmin.LdifName;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Centralise common patterns to manage roles with a user admin */
public class UserAdminUtils {

	/** Retrieves a {@link Role} given a LDAP name */
	public final static Role getRole(UserAdmin userAdmin, LdapName dn) {
		Role role = userAdmin.getRole(dn.toString());
		return role;
	}

	/** Retrieves the unique local username given a {@link User}. */
	public final static String getUsername(User user) {
		String username = null;
		if (user instanceof Group)
			username = getProperty(user, LdifName.cn.name());
		else
			username = getProperty(user, LdifName.uid.name());
		return username;
	}

	/**
	 * Easily retrieves one of the {@link Role}'s property or an empty String if
	 * the requested property is not defined
	 */
	public final static String getProperty(Role role, String key) {
		Object obj = role.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	// CENTRALIZE SOME METHODS UNTIL API IS STABLE
	/** Simply checks if current user is registered */
	public static boolean isRegistered() {
		return !CurrentUser.isAnonymous();
	}

	/** Simply checks if current user as a home */
	public static boolean hasHome() {
		return isRegistered();
	}

	// SELF HELPERS
	/** Simply retrieves the current logged-in user display name. */
	public static User getCurrentUser(UserAdmin userAdmin) {
		return (User) getRole(userAdmin, getCurrentUserLdapName());
	}

	/** Simply retrieves the current logged-in user display name. */
	public static String getCurrentUserDisplayName(UserAdmin userAdmin) {
		String username = getCurrentUsername();
		return getUserDisplayName(userAdmin, username);
	}

	/** Simply retrieves the current logged-in user display name. */
	public static String getCurrentUserMail(UserAdmin userAdmin) {
		String username = getCurrentUsername();
		return getUserMail(userAdmin, username);
	}

	/** Returns the local name of the current connected user */
	public final static String getUsername(UserAdmin userAdmin) {
		LdapName dn = getCurrentUserLdapName();
		return getUsername((User) getRole(userAdmin, dn));
	}

	/** Returns true if the current user is in the specified role */
	public static boolean isUserInRole(String role) {
		Set<String> roles = CurrentUser.roles();
		return roles.contains(role);
	}

	/** Simply checks if current user is the same as the passed one */
	public static boolean isCurrentUser(User user) {
		String userName = getProperty(user, LdifName.dn.name());
		try {
			LdapName selfUserName = getCurrentUserLdapName();
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName))
				return true;
			else
				return false;
		} catch (InvalidNameException e) {
			throw new CmsException("User " + user + " has an unvalid dn: "
					+ userName, e);
		}
	}

	public final static LdapName getCurrentUserLdapName() {
		String name = getCurrentUsername();
		return getLdapName(name);
	}

	/** Simply retrieves username for current user, generally a LDAP dn */
	public static String getCurrentUsername() {
		Subject subject = currentSubject();
		String name = subject.getPrincipals(X500Principal.class).iterator()
				.next().toString();
		return name;
	}

	/**
	 * Fork of the {@link CurrentUser#currentSubject} method that is private.
	 * TODO Enhance and factorize
	 */
	private static Subject currentSubject() {
		CmsView cmsView = CmsUtils.getCmsView();
		if (cmsView != null)
			return cmsView.getSubject();
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject != null)
			return subject;
		throw new RuntimeException("Cannot find related subject");
	}

	// HOME MANAGEMENT
	/**
	 * Simply retrieves the *relative* path to the current user home node from
	 * the base home node
	 */
	public static String getCurrentUserHomeRelPath() {
		return getHomeRelPath(getCurrentUsername());
	}

	/**
	 * Simply retrieves the *relative* path to the home node of a user given its
	 * userName
	 */
	public static String getHomeRelPath(String userName) {
		String id = getUserUid(userName);
		String currHomePath = JcrUtils.firstCharsToPath(id, 2) + "/" + id;
		return currHomePath;
	}

	// HELPERS TO RETRIEVE REMARKABLE PROPERTIES
	/** Simply retrieves the user uid from his dn with no useradmin */
	public static String getUserUid(String dn) {
		LdapName ldapName = getLdapName(dn);
		Rdn last = ldapName.getRdn(ldapName.size() - 1);
		if (last.getType().toLowerCase().equals(LdifName.uid.name())
				|| last.getType().toLowerCase().equals(LdifName.cn.name()))
			return (String) last.getValue();
		else
			throw new CmsException("Cannot retrieve user uid, "
					+ "non valid dn: " + dn);
	}

	/**
	 * Returns the local username if no user with this dn is found or if the
	 * found user has no defined display name
	 */
	public static String getUserDisplayName(UserAdmin userAdmin, String dn) {
		Role user = getRole(userAdmin, getLdapName(dn));
		if (user == null)
			return getUserUid(dn);
		String displayName = getProperty(user, LdifName.displayName.name());
		if (EclipseUiUtils.isEmpty(displayName))
			displayName = getProperty(user, LdifName.cn.name());
		if (EclipseUiUtils.isEmpty(displayName))
			return getUserUid(dn);
		else
			return displayName;
	}

	/**
	 * Returns null if no user with this dn is found or if the found user has no
	 * defined mail
	 */
	public static String getUserMail(UserAdmin userAdmin, String dn) {
		Role user = getRole(userAdmin, getLdapName(dn));
		if (user == null)
			return null;
		else
			return getProperty(user, LdifName.mail.name());
	}

	// VARIOUS UI HELPERS
	public final static String buildDefaultCn(String firstName, String lastName) {
		return (firstName.trim() + " " + lastName.trim() + " ").trim();
	}

	/** Simply retrieves a display name of the relevant domain */
	public final static String getDomainName(User user) {
		String dn = user.getName();
		if (dn.endsWith(AuthConstants.ROLES_BASEDN))
			return "System roles";
		try {
			LdapName name = new LdapName(dn);
			List<Rdn> rdns = name.getRdns();
			String dname = null;
			int i = 0;
			loop: while (i < rdns.size()) {
				Rdn currrRdn = rdns.get(i);
				if (!LdifName.dc.name().equals(currrRdn.getType()))
					break loop;
				else {
					String currVal = (String) currrRdn.getValue();
					dname = dname == null ? currVal : currVal + "." + dname;
				}
				i++;
			}
			return dname;
		} catch (InvalidNameException e) {
			throw new CmsException("Unable to get domain name for " + dn, e);
		}
	}

	// Local Helpers
	/** Simply retrieves a LDAP name from a dn with no exception */
	public static LdapName getLdapName(String dn) {
		try {
			return new LdapName(dn);
		} catch (InvalidNameException e) {
			throw new CmsException("Cannot parse LDAP name " + dn, e);
		}
	}
}