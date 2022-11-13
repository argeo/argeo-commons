package org.argeo.cms.internal.runtime;

import static org.argeo.api.acr.ldap.LdapAttr.cn;
import static org.argeo.api.acr.ldap.LdapAttr.description;
import static org.argeo.api.acr.ldap.LdapAttr.owner;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.api.acr.ldap.NamingUtils;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.directory.CmsGroup;
import org.argeo.api.cms.directory.CmsUser;
import org.argeo.api.cms.directory.CmsUserManager;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.api.cms.transaction.WorkTransaction;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.cms.directory.ldap.LdapEntry;
import org.argeo.cms.directory.ldap.SharedSecret;
import org.argeo.cms.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.cms.osgi.useradmin.TokenUtils;
import org.argeo.cms.runtime.DirectoryConf;
import org.osgi.framework.InvalidSyntaxException;
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
	private final static CmsLog log = CmsLog.getLog(CmsUserManagerImpl.class);

	private UserAdmin userAdmin;
//	private Map<String, String> serviceProperties;
	private WorkTransaction userTransaction;

	private final String[] knownProps = { LdapAttr.cn.name(), LdapAttr.sn.name(), LdapAttr.givenName.name(),
			LdapAttr.uid.name() };

//	private Map<UserDirectory, Hashtable<String, Object>> userDirectories = Collections
//			.synchronizedMap(new LinkedHashMap<>());

	private Set<UserDirectory> userDirectories = new HashSet<>();

	public void start() {
		log.debug(() -> "CMS user manager available");
	}

	public void stop() {

	}

	@Override
	public String getMyMail() {
		return getUserMail(CurrentUser.getUsername());
	}

	@Override
	public Role[] getRoles(String filter) {
		try {
			return userAdmin.getRoles(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter " + filter, e);
		}
	}

	// ALL USER: WARNING access to this will be later reduced

	/** Retrieve a user given his dn, or <code>null</code> if it doesn't exist. */
	public CmsUser getUser(String dn) {
		return (CmsUser) getUserAdmin().getRole(dn);
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

	public Set<CmsUser> listUsersInGroup(String groupDn, String filter) {
		Group group = (Group) userAdmin.getRole(groupDn);
		if (group == null)
			throw new IllegalArgumentException("Group " + groupDn + " not found");
		Set<CmsUser> users = new HashSet<>();
		addUsers(users, group, filter);
		return users;
	}

//	@Override
//	public Set<User> listAccounts(HierarchyUnit hierarchyUnit, boolean deep) {
//		if(!hierarchyUnit.isFunctional())
//			throw new IllegalArgumentException("Hierarchy unit "+hierarchyUnit.getBase()+" is not functional");
//		UserDirectory directory = (UserDirectory)hierarchyUnit.getDirectory();
//		Set<User> res = new HashSet<>();
//		for(HierarchyUnit technicalHu:hierarchyUnit.getDirectHierarchyUnits(false)) {
//			if(technicalHu.isFunctional())
//				continue;
//			for(Role role:directory.getHierarchyUnitRoles(technicalHu, null, false)) {
//				if(role)
//			}
//		}
//		return res;
//	}

	/** Recursively add users to list */
	private void addUsers(Set<CmsUser> users, Group group, String filter) {
		Role[] roles = group.getMembers();
		for (Role role : roles) {
			if (role.getType() == Role.GROUP) {
				addUsers(users, (CmsGroup) role, filter);
			} else if (role.getType() == Role.USER) {
				if (match(role, filter))
					users.add((CmsUser) role);
			} else {
				// ignore
			}
		}
	}

	public List<CmsUser> listGroups(String filter, boolean includeUsers, boolean includeSystemRoles) {
		Role[] roles = null;
		try {
			roles = getUserAdmin().getRoles(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unable to get roles with filter: " + filter, e);
		}

		List<CmsUser> users = new ArrayList<>();
		for (Role role : roles) {
			if ((includeUsers && role.getType() == Role.USER || role.getType() == Role.GROUP) && !users.contains(role)
					&& (includeSystemRoles
							|| !role.getName().toLowerCase().endsWith(CmsConstants.SYSTEM_ROLES_BASEDN))) {
				if (match(role, filter))
					users.add((CmsUser) role);
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
	public CmsUser getUserFromLocalId(String localId) {
		CmsUser user = (CmsUser) getUserAdmin().getUser(LdapAttr.uid.name(), localId);
		if (user == null)
			user = (CmsUser) getUserAdmin().getUser(LdapAttr.cn.name(), localId);
		return user;
	}

	@Override
	public String buildDefaultDN(String localId, int type) {
		return buildDistinguishedName(localId, getDefaultDomainName(), type);
	}

	/*
	 * EDITION
	 */
	@Override
	public CmsUser createUser(String username, Map<String, Object> properties, Map<String, Object> credentials) {
		try {
			userTransaction.begin();
			CmsUser user = (CmsUser) userAdmin.createRole(username, Role.USER);
			if (properties != null) {
				for (String key : properties.keySet())
					user.getProperties().put(key, properties.get(key));
			}
			if (credentials != null) {
				for (String key : credentials.keySet())
					user.getCredentials().put(key, credentials.get(key));
			}
			userTransaction.commit();
			return user;
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot create user " + username, e);
		}
	}

	@Override
	public CmsGroup getOrCreateGroup(HierarchyUnit groups, String commonName) {
		try {
			String dn = LdapAttr.cn.name() + "=" + commonName + "," + groups.getBase();
			CmsGroup group = (CmsGroup) getUserAdmin().getRole(dn);
			if (group != null)
				return group;
			userTransaction.begin();
			group = (CmsGroup) userAdmin.createRole(dn, Role.GROUP);
			userTransaction.commit();
			return group;
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot create group " + commonName + " in " + groups, e);
		}
	}

	@Override
	public CmsGroup getOrCreateSystemRole(HierarchyUnit roles, QName systemRole) {
		try {
			String dn = LdapAttr.cn.name() + "=" + NamespaceUtils.toPrefixedName(systemRole) + "," + roles.getBase();
			CmsGroup group = (CmsGroup) getUserAdmin().getRole(dn);
			if (group != null)
				return group;
			userTransaction.begin();
			group = (CmsGroup) userAdmin.createRole(dn, Role.GROUP);
			userTransaction.commit();
			return group;
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException("Cannot create system role " + systemRole + " in " + roles, e);
		}
	}

	@Override
	public HierarchyUnit getOrCreateHierarchyUnit(UserDirectory directory, String path) {
		HierarchyUnit hi = directory.getHierarchyUnit(path);
		if (hi != null)
			return hi;
		try {
			userTransaction.begin();
			HierarchyUnit hierarchyUnit = directory.createHierarchyUnit(path);
			userTransaction.commit();
			return hierarchyUnit;
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot create hierarchy unit " + path + " in directory " + directory, e1);
		}
	}

	@Override
	public void addObjectClasses(Role role, Set<String> objectClasses, Map<String, Object> additionalProperties) {
		try {
			userTransaction.begin();
			LdapEntry.addObjectClasses(role.getProperties(), objectClasses);
			for (String key : additionalProperties.keySet()) {
				role.getProperties().put(key, additionalProperties.get(key));
			}
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add object classes " + objectClasses + " to " + role, e1);
		}
	}

	@Override
	public void addObjectClasses(HierarchyUnit hierarchyUnit, Set<String> objectClasses,
			Map<String, Object> additionalProperties) {
		try {
			userTransaction.begin();
			LdapEntry.addObjectClasses(hierarchyUnit.getProperties(), objectClasses);
			for (String key : additionalProperties.keySet()) {
				hierarchyUnit.getProperties().put(key, additionalProperties.get(key));
			}
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add object classes " + objectClasses + " to " + hierarchyUnit, e1);
		}
	}

	@Override
	public void edit(Runnable action) {
		Objects.requireNonNull(action);
		try {
			userTransaction.begin();
			action.run();
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot edit", e1);
		}
	}

	@Override
	public void addMember(CmsGroup group, Role role) {
		try {
			userTransaction.begin();
			group.addMember(role);
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add object classes " + role + " to group " + group, e1);
		}
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
		for (UserDirectory userDirectory : userDirectories) {
			Boolean readOnly = userDirectory.isReadOnly();
			String baseDn = userDirectory.getBase();

			if (onlyWritable && readOnly)
				continue;
			if (baseDn.equalsIgnoreCase(CmsConstants.SYSTEM_ROLES_BASEDN))
				continue;
			if (baseDn.equalsIgnoreCase(CmsConstants.TOKENS_BASEDN))
				continue;
			dns.put(baseDn, DirectoryConf.propertiesAsUri(userDirectory.getProperties()).toString());

		}
		return dns;
	}

	public Set<UserDirectory> getUserDirectories() {
		TreeSet<UserDirectory> res = new TreeSet<>((o1, o2) -> o1.getBase().compareTo(o2.getBase()));
		res.addAll(userDirectories);
		return res;
	}

	public String buildDistinguishedName(String localId, String baseDn, int type) {
		Map<String, String> dns = getKnownBaseDns(true);
		Dictionary<String, ?> props = DirectoryConf.uriAsProperties(dns.get(baseDn));
		String dn = null;
		if (Role.GROUP == type)
			dn = LdapAttr.cn.name() + "=" + localId + "," + DirectoryConf.groupBase.getValue(props) + "," + baseDn;
		else if (Role.USER == type)
			dn = LdapAttr.uid.name() + "=" + localId + "," + DirectoryConf.userBase.getValue(props) + "," + baseDn;
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
		User user = (User) userAdmin.getUser(LdapAttr.mail.name(), email);
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
				if (!userTransaction.isNoTransactionStatus())
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
			String dn = cn + "=" + token + "," + CmsConstants.TOKENS_BASEDN;
			Group tokenGroup = (Group) userAdmin.getRole(dn);
			String ldapDate = NamingUtils.instantToLdapDate(ZonedDateTime.now(ZoneOffset.UTC));
			tokenGroup.getProperties().put(description.name(), ldapDate);
			userTransaction.commit();
			if (log.isDebugEnabled())
				log.debug("Token " + token + " expired.");
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
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
		Set<String> tokens = TokenUtils.tokensUsed(subject, CmsConstants.TOKENS_BASEDN);
		for (String token : tokens)
			expireAuthToken(token);
	}

	@Override
	public void addAuthToken(String userDn, String token, Integer hours, String... roles) {
		addAuthToken(userDn, token, ZonedDateTime.now().plusHours(hours), roles);
	}

	@Override
	public void addAuthToken(String userDn, String token, ZonedDateTime expiryDate, String... roles) {
		try {
			userTransaction.begin();
			User user = (User) userAdmin.getRole(userDn);
			String tokenDn = cn + "=" + token + "," + CmsConstants.TOKENS_BASEDN;
			Group tokenGroup = (Group) userAdmin.createRole(tokenDn, Role.GROUP);
			if (roles != null)
				for (String role : roles) {
					Role r = userAdmin.getRole(role);
					if (r != null)
						tokenGroup.addMember(r);
					else {
						if (!role.equals(CmsConstants.ROLE_USER)) {
							throw new IllegalStateException(
									"Cannot add role " + role + " to token " + token + " for " + userDn);
						}
					}
				}
			tokenGroup.getProperties().put(owner.name(), user.getName());
			if (expiryDate != null) {
				String ldapDate = NamingUtils.instantToLdapDate(expiryDate);
				tokenGroup.getProperties().put(description.name(), ldapDate);
			}
			userTransaction.commit();
		} catch (Exception e1) {
			try {
				if (!userTransaction.isNoTransactionStatus())
					userTransaction.rollback();
			} catch (Exception e2) {
				if (log.isTraceEnabled())
					log.trace("Cannot rollback transaction", e2);
			}
			throw new RuntimeException("Cannot add token", e1);
		}
	}

	@Override
	public UserDirectory getDirectory(Role user) {
		String name = user.getName();
		NavigableMap<String, UserDirectory> possible = new TreeMap<>();
		for (UserDirectory userDirectory : userDirectories) {
			if (name.endsWith(userDirectory.getBase())) {
				possible.put(userDirectory.getBase(), userDirectory);
			}
		}
		if (possible.size() == 0)
			throw new IllegalStateException("No user directory found for user " + name);
		return possible.lastEntry().getValue();
	}

//	public User createUserFromPerson(Node person) {
//		String email = JcrUtils.get(person, LdapAttrs.mail.property());
//		String dn = buildDefaultDN(email, Role.USER);
//		User user;
//		try {
//			userTransaction.begin();
//			user = (User) userAdmin.createRole(dn, Role.USER);
//			Dictionary<String, Object> userProperties = user.getProperties();
//			String name = JcrUtils.get(person, LdapAttrs.displayName.property());
//			userProperties.put(LdapAttrs.cn.name(), name);
//			userProperties.put(LdapAttrs.displayName.name(), name);
//			String givenName = JcrUtils.get(person, LdapAttrs.givenName.property());
//			String surname = JcrUtils.get(person, LdapAttrs.sn.property());
//			userProperties.put(LdapAttrs.givenName.name(), givenName);
//			userProperties.put(LdapAttrs.sn.name(), surname);
//			userProperties.put(LdapAttrs.mail.name(), email.toLowerCase());
//			userTransaction.commit();
//		} catch (Exception e) {
//			try {
//				userTransaction.rollback();
//			} catch (Exception e1) {
//				log.error("Could not roll back", e1);
//			}
//			if (e instanceof RuntimeException)
//				throw (RuntimeException) e;
//			else
//				throw new RuntimeException("Cannot create user", e);
//		}
//		return user;
//	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

//	public UserTransaction getUserTransaction() {
//		return userTransaction;
//	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;

		if (userAdmin instanceof AggregatingUserAdmin) {
			userDirectories = ((AggregatingUserAdmin) userAdmin).getUserDirectories();
		} else {
			throw new IllegalArgumentException("Only " + AggregatingUserAdmin.class.getName() + " is supported.");
		}

//		this.serviceProperties = serviceProperties;
	}

	public void setUserTransaction(WorkTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

//	public void addUserDirectory(UserDirectory userDirectory, Map<String, Object> properties) {
//		userDirectories.put(userDirectory, new Hashtable<>(properties));
//	}
//
//	public void removeUserDirectory(UserDirectory userDirectory, Map<String, Object> properties) {
//		userDirectories.remove(userDirectory);
//	}

}
