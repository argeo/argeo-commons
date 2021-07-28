package org.argeo.osgi.useradmin;

import static org.argeo.naming.LdapAttrs.objectClass;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * A user admin based on a LDAP server. Requires a {@link TransactionManager}
 * and an open transaction for write access.
 */
public class LdapUserAdmin extends AbstractUserDirectory {
	private LdapConnection ldapConnection;

	public LdapUserAdmin(Dictionary<String, ?> properties) {
		this(properties, false);
	}

	public LdapUserAdmin(Dictionary<String, ?> properties, boolean scoped) {
		super(null, properties, scoped);
		ldapConnection = new LdapConnection(getUri().toString(), properties);
	}

	public void destroy() {
		ldapConnection.destroy();
	}

	@Override
	protected AbstractUserDirectory scope(User user) {
		Dictionary<String, Object> credentials = user.getCredentials();
		String username = (String) credentials.get(SHARED_STATE_USERNAME);
		if (username == null)
			username = user.getName();
		Dictionary<String, Object> properties = cloneProperties();
		properties.put(Context.SECURITY_PRINCIPAL, username.toString());
		Object pwdCred = credentials.get(SHARED_STATE_PASSWORD);
		byte[] pwd = (byte[]) pwdCred;
		if (pwd != null) {
			char[] password = DigestUtils.bytesToChars(pwd);
			properties.put(Context.SECURITY_CREDENTIALS, new String(password));
		} else {
			properties.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
		}
		return new LdapUserAdmin(properties, true);
	}

//	protected InitialLdapContext getLdapContext() {
//		return initialLdapContext;
//	}

	@Override
	protected Boolean daoHasRole(LdapName dn) {
		try {
			return daoGetRole(dn) != null;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	@Override
	protected DirectoryUser daoGetRole(LdapName name) throws NameNotFoundException {
		try {
			Attributes attrs = ldapConnection.getAttributes(name);
			if (attrs.size() == 0)
				return null;
			int roleType = roleType(name);
			LdifUser res;
			if (roleType == Role.GROUP)
				res = new LdifGroup(this, name, attrs);
			else if (roleType == Role.USER)
				res = new LdifUser(this, name, attrs);
			else
				throw new UserDirectoryException("Unsupported LDAP type for " + name);
			return res;
		} catch (NameNotFoundException e) {
			throw e;
		} catch (NamingException e) {
			return null;
		}
	}

	@Override
	protected List<DirectoryUser> doGetRoles(Filter f) {
		ArrayList<DirectoryUser> res = new ArrayList<DirectoryUser>();
		try {
			String searchFilter = f != null ? f.toString()
					: "(|(" + objectClass + "=" + getUserObjectClass() + ")(" + objectClass + "="
							+ getGroupObjectClass() + "))";
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			LdapName searchBase = getBaseDn();
			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			results: while (results.hasMoreElements()) {
				SearchResult searchResult = results.next();
				Attributes attrs = searchResult.getAttributes();
				Attribute objectClassAttr = attrs.get(objectClass.name());
				LdapName dn = toDn(searchBase, searchResult);
				LdifUser role;
				if (objectClassAttr.contains(getGroupObjectClass())
						|| objectClassAttr.contains(getGroupObjectClass().toLowerCase()))
					role = new LdifGroup(this, dn, attrs);
				else if (objectClassAttr.contains(getUserObjectClass())
						|| objectClassAttr.contains(getUserObjectClass().toLowerCase()))
					role = new LdifUser(this, dn, attrs);
				else {
//					log.warn("Unsupported LDAP type for " + searchResult.getName());
					continue results;
				}
				res.add(role);
			}
			return res;
		} catch (AuthenticationNotSupportedException e) {
			// ignore (typically an unsupported anonymous bind)
			// TODO better logging
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserDirectoryException("Cannot get roles for filter " + f, e);
		}
	}

	private LdapName toDn(LdapName baseDn, Binding binding) throws InvalidNameException {
		return new LdapName(binding.isRelative() ? binding.getName() + "," + baseDn : binding.getName());
	}

	@Override
	protected List<LdapName> getDirectGroups(LdapName dn) {
		List<LdapName> directGroups = new ArrayList<LdapName>();
		try {
			String searchFilter = "(&(" + objectClass + "=" + getGroupObjectClass() + ")(" + getMemberAttributeId()
					+ "=" + dn + "))";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			LdapName searchBase = getBaseDn();
			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			while (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.nextElement();
				directGroups.add(toDn(searchBase, searchResult));
			}
			return directGroups;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot populate direct members of " + dn, e);
		}
	}

	@Override
	protected void prepare(UserDirectoryWorkingCopy wc) {
		try {
			ldapConnection.prepareChanges(wc);
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot prepare LDAP", e);
		}
	}

	@Override
	protected void commit(UserDirectoryWorkingCopy wc) {
		try {
			ldapConnection.commitChanges(wc);
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot commit LDAP", e);
		}
	}

	@Override
	protected void rollback(UserDirectoryWorkingCopy wc) {
		// prepare not impacting
	}

}
