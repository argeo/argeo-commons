package org.argeo.cms.internal.auth;

import static org.argeo.naming.LdapAttrs.cn;
import static org.argeo.naming.LdapAttrs.description;
import static org.argeo.naming.LdapAttrs.owner;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Node;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsUserManager;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.naming.LdapAttrs;
import org.argeo.naming.NamingUtils;
import org.argeo.naming.SharedSecret;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.TokenUtils;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Canonical implementation of the people {@link CmsUserManager}. Wraps
 * interaction with users and groups.
 * 
 * In a *READ-ONLY* mode. We want to be able to:
 * <ul>
 * <li>Retrieve my user and corresponding information (main info,
 * groups...)</li>
 * <li>List all local groups (not the system roles)</li>
 * <li>If sufficient rights: retrieve a given user and its information</li>
 * </ul>
 */
public class CmsUserManagerImpl implements CmsUserManager {
	private final static Log log = LogFactory.getLog(CmsUserManagerImpl.class);

	private UserAdmin userAdmin;
	@Deprecated
	private ServiceReference<UserAdmin> userAdminServiceReference;
	private Map<String, String> serviceProperties;
	private UserTransaction userTransaction;

	@Override
	public String getMyMail() {
		return getUserMail(CurrentUser.getUsername());
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		return userAdmin.getRoles(filter);
	}

	// ALL USER: WARNING access to this will be later reduced

	/** Retrieve a user given his dn */
	public User getUser(String dn) {
		return (User) getUserAdmin().getRole(dn);
	}

	/** Can be a group or a user */
	public String getUserDisplayName(String dn) {
		// FIXME: during initialisation phase, the system logs "admin" as user
		// name rather than the corresponding dn
		if ("admin".equals(dn))
			return "System Administrator";
		else
			return UserAdminUtils.getUserDisplayName(getUserAdmin(), dn);
	}

	@Override
	public String getUserMail(String dn) {
		return UserAdminUtils.getUserMail(getUserAdmin(), dn);
	}

	/** Lists all roles of the given user */
	@Override
	public String[] getUserRoles(String dn) {
		Authorization currAuth = getUserAdmin().getAuthorization(getUser(dn));
		return currAuth.getRoles();
	}

	@Override
	public boolean isUserInRole(String userDn, String roleDn) {
		String[] roles = getUserRoles(userDn);
		for (String role : roles) {
			if (role.equalsIgnoreCase(roleDn))
				return true;
		}
		return false;
	}

	private final String[] knownProps = { LdapAttrs.cn.name(), LdapAttrs.sn.name(), LdapAttrs.givenName.name(),
			LdapAttrs.uid.name() };

	public Set<User> listUsersInGroup(String groupDn, String filter) {
		Group group = (Group) userAdmin.getRole(groupDn);
		if (group == null)
			throw new IllegalArgumentException("Group " + groupDn + " not found");
		Set<User> users = new HashSet<User>();
		addUsers(users, group, filter);
		return users;
	}

	/** Recursively add users to list */
	private void addUsers(Set<User> users, Group group, String filter) {
		Role[] roles = group.getMembers();
		for (Role role : roles) {
			if (role.getType() == Role.GROUP) {
				addUsers(users, (Group) role, filter);
			} else if (role.getType() == Role.USER) {
				if (match(role, filter))
					users.add((User) role);
			} else {
				// ignore
			}
		}
	}

	public List<User> listGroups(String filter, boolean includeUsers, boolean includeSystemRoles) {
		Role[] roles = null;
		try {
			roles = getUserAdmin().getRoles(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unable to get roles with filter: " + filter, e);
		}

		List<User> users = new ArrayList<User>();
		for (Role role : roles) {
			if ((includeUsers && role.getType() == Role.USER || role.getType() == Role.GROUP) && !users.contains(role)
					&& (includeSystemRoles || !role.getName().toLowerCase().endsWith(NodeConstants.ROLES_BASEDN))) {
				if (match(role, filter))
					users.add((User) role);
			}
		}
		return users;
	}

	private boolean match(Role role, String filter) {
		boolean doFilter = filter != null && !"".equals(filter);
		if (doFilter) {
			for (String prop : knownProps) {
				Object currProp = null;
				try {
					currProp = role.getProperties().get(prop);
				} catch (Exception e) {
					throw e;
				}
				if (currProp != null) {
					String currPropStr = ((String) currProp).toLowerCase();
					if (currPropStr.contains(filter.toLowerCase())) {
						return true;
					}
				}
			}
			return false;
		} else
			return true;
	}

	@Override
	public User getUserFromLocalId(String localId) {
		User user = getUserAdmin().getUser(LdapAttrs.uid.name(), localId);
		if (user == null)
			user = getUserAdmin().getUser(LdapAttrs.cn.name(), localId);
		return user;
	}

	@Override
	public String buildDefaultDN(String localId, int type) {
		return buildDistinguishedName(localId, getDefaultDomainName(), type);
	}

	@Override
	public String getDefaultDomainName() {
		Map<String, String> dns = getKnownBaseDns(true);
		if (dns.size() == 1)
			return dns.keySet().iterator().next();
		else
			throw new IllegalStateException("Current context contains " + dns.size() + " base dns: "
					+ dns.keySet().toString() + ". Unable to chose a default one.");
	}

	public Map<String, String> getKnownBaseDns(boolean onlyWritable) {
		Map<String, String> dns = new HashMap<String, String>();
		String[] propertyKeys = userAdminServiceReference != null ? userAdminServiceReference.getPropertyKeys()
				: serviceProperties.keySet().toArray(new String[serviceProperties.size()]);
		for (String uri : propertyKeys) {
			if (!uri.startsWith("/"))
				continue;
			Dictionary<String, ?> props = UserAdminConf.uriAsProperties(uri);
			String readOnly = UserAdminConf.readOnly.getValue(props);
			String baseDn = UserAdminConf.baseDn.getValue(props);

			if (onlyWritable && "true".equals(readOnly))
				continue;
			if (baseDn.equalsIgnoreCase(NodeConstants.ROLES_BASEDN))
				continue;
			if (baseDn.equalsIgnoreCase(NodeConstants.TOKENS_BASEDN))
				continue;
			dns.put(baseDn, uri);
		}
		return dns;
	}

	public String buildDistinguishedName(String localId, String baseDn, int type) {
		Map<String, String> dns = getKnownBaseDns(true);
		Dictionary<String, ?> props = UserAdminConf.uriAsProperties(dns.get(baseDn));
		String dn = null;
		if (Role.GROUP == type)
			dn = LdapAttrs.cn.name() + "=" + localId + "," + UserAdminConf.groupBase.getValue(props) + "," + baseDn;
		else if (Role.USER == type)
			dn = LdapAttrs.uid.name() + "=" + localId + "," + UserAdminConf.userBase.getValue(props) + "," + baseDn;
		else
			throw new IllegalStateException("Unknown role type. " + "Cannot deduce dn for " + localId);
		return dn;
	}

	@Override
	public void changeOwnPassword(char[] oldPassword, char[] newPassword) {
		String name = CurrentUser.getUsername();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Invalid user dn " + name, e);
		}
		User user = (User) userAdmin.getRole(dn.toString());
		if (!user.hasCredential(null, oldPassword))
			throw new IllegalArgumentException("Invalid password");
		if (Arrays.equals(newPassword, new char[0]))
			throw new IllegalArgumentException("New password empty");
		try {
			userTransaction.begin();
			user.getCredentials().put(null, newPassword);
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot change password", e);
		}
	}

	public void resetPassword(String username, char[] newPassword) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Invalid user dn " + username, e);
		}
		User user = (User) userAdmin.getRole(dn.toString());
		if (Arrays.equals(newPassword, new char[0]))
			throw new IllegalArgumentException("New password empty");
		try {
			userTransaction.begin();
			user.getCredentials().put(null, newPassword);
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot change password", e);
		}
	}

	public String addSharedSecret(String email, int hours) {
		User user = (User) userAdmin.getUser(LdapAttrs.mail.name(), email);
		try {
			userTransaction.begin();
			String uuid = UUID.randomUUID().toString();
			SharedSecret sharedSecret = new SharedSecret(hours, uuid);
			user.getCredentials().put(SharedSecret.X_SHARED_SECRET, sharedSecret.toAuthPassword());
			String tokenStr = sharedSecret.getAuthInfo() + '$' + sharedSecret.getAuthValue();
			userTransaction.commit();
			return tokenStr;
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot change password", e);
		}
	}

	@Deprecated
	public String addSharedSecret(String username, String authInfo, String authToken) {
		try {
			userTransaction.begin();
			User user = (User) userAdmin.getRole(username);
			SharedSecret sharedSecret = new SharedSecret(authInfo, authToken);
			user.getCredentials().put(SharedSecret.X_SHARED_SECRET, sharedSecret.toAuthPassword());
			String tokenStr = sharedSecret.getAuthInfo() + '$' + sharedSecret.getAuthValue();
			userTransaction.commit();
			return tokenStr;
		} catch (Exception e1) {
			try {
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add shared secret", e1);
		}
	}

	@Override
	public void expireAuthToken(String token) {
		try {
			userTransaction.begin();
			String dn = cn + "=" + token + "," + NodeConstants.TOKENS_BASEDN;
			Group tokenGroup = (Group) userAdmin.getRole(dn);
			String ldapDate = NamingUtils.instantToLdapDate(ZonedDateTime.now(ZoneOffset.UTC));
			tokenGroup.getProperties().put(description.name(), ldapDate);
			userTransaction.commit();
			if (log.isDebugEnabled())
				log.debug("Token " + token + " expired.");
		} catch (Exception e1) {
			try {
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot expire token", e1);
		}
	}

	@Override
	public void expireAuthTokens(Subject subject) {
		Set<String> tokens = TokenUtils.tokensUsed(subject, NodeConstants.TOKENS_BASEDN);
		for (String token : tokens)
			expireAuthToken(token);
	}

	@Override
	public void addAuthToken(String userDn, String token, Integer hours, String... roles) {
		try {
			userTransaction.begin();
			User user = (User) userAdmin.getRole(userDn);
			String tokenDn = cn + "=" + token + "," + NodeConstants.TOKENS_BASEDN;
			Group tokenGroup = (Group) userAdmin.createRole(tokenDn, Role.GROUP);
			for (String role : roles) {
				Role r = userAdmin.getRole(role);
				if (r != null)
					tokenGroup.addMember(r);
				else {
					if (!role.equals(NodeConstants.ROLE_USER)) {
						throw new IllegalStateException(
								"Cannot add role " + role + " to token " + token + " for " + userDn);
					}
				}
			}
			tokenGroup.getProperties().put(owner.name(), user.getName());
			if (hours != null) {
				String ldapDate = NamingUtils.instantToLdapDate(ZonedDateTime.now().plusHours(hours));
				tokenGroup.getProperties().put(description.name(), ldapDate);
			}
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add token", e1);
		}
	}

	public User createUserFromPerson(Node person) {
		String email = JcrUtils.get(person, LdapAttrs.mail.property());
		String dn = buildDefaultDN(email, Role.USER);
		User user;
		try {
			userTransaction.begin();
			user = (User) userAdmin.createRole(dn, Role.USER);
			Dictionary<String, Object> userProperties = user.getProperties();
			String name = JcrUtils.get(person, LdapAttrs.displayName.property());
			userProperties.put(LdapAttrs.cn.name(), name);
			userProperties.put(LdapAttrs.displayName.name(), name);
			String givenName = JcrUtils.get(person, LdapAttrs.givenName.property());
			String surname = JcrUtils.get(person, LdapAttrs.sn.property());
			userProperties.put(LdapAttrs.givenName.name(), givenName);
			userProperties.put(LdapAttrs.sn.name(), surname);
			userProperties.put(LdapAttrs.mail.name(), email.toLowerCase());
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot create user", e);
		}
		return user;
	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin, Map<String, String> serviceProperties) {
		this.userAdmin = userAdmin;
		this.serviceProperties = serviceProperties;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}
