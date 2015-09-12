package org.argeo.osgi.useradmin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

public class LdapUserAdmin extends AbstractUserDirectory {
	private final static Log log = LogFactory.getLog(LdapUserAdmin.class);

	private String baseDn = "dc=example,dc=com";
	private InitialLdapContext initialLdapContext = null;

	public LdapUserAdmin(String uri) {
		try {
			setUri(new URI(uri));
			Hashtable<String, Object> connEnv = new Hashtable<String, Object>();
			connEnv.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			connEnv.put(Context.PROVIDER_URL, getUri().toString());
			connEnv.put("java.naming.ldap.attributes.binary", "userPassword");
			// connEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
			// connEnv.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
			// connEnv.put(Context.SECURITY_CREDENTIALS, "secret");

			initialLdapContext = new InitialLdapContext(connEnv, null);
			// StartTlsResponse tls = (StartTlsResponse) ctx
			// .extendedOperation(new StartTlsRequest());
			// tls.negotiate();
			initialLdapContext.addToEnvironment(
					Context.SECURITY_AUTHENTICATION, "simple");
			initialLdapContext.addToEnvironment(Context.SECURITY_PRINCIPAL,
					"uid=admin,ou=system");
			initialLdapContext.addToEnvironment(Context.SECURITY_CREDENTIALS,
					"secret");
			LdapContext ldapContext = (LdapContext) initialLdapContext
					.lookup("uid=root,ou=users,dc=example,dc=com");
			log.debug(initialLdapContext.getAttributes(
					"uid=root,ou=users,dc=example,dc=com").get("cn"));
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot connect to LDAP", e);
		}
	}

	public void destroy() {
		try {
			// tls.close();
			initialLdapContext.close();
		} catch (NamingException e) {
			log.error("Cannot destroy LDAP user admin", e);
		}
	}

	@Override
	public Role createRole(String name, int type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeRole(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Role getRole(String name) {
		try {
			Attributes attrs = initialLdapContext.getAttributes(name);
			LdifUser res;
			if (attrs.get("objectClass").contains("groupOfNames"))
				res = new LdifGroup(this, new LdapName(name), attrs);
			else if (attrs.get("objectClass").contains("inetOrgPerson"))
				res = new LdifUser(this, new LdapName(name), attrs);
			else
				throw new UserDirectoryException("Unsupported LDAP type for "
						+ name);
			return res;
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot get role for " + name, e);
		}
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		try {
			String searchFilter = filter;
			if (searchFilter == null)
				searchFilter = "(|(objectClass=inetOrgPerson)(objectClass=groupOfNames))";
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String searchBase = baseDn;
			NamingEnumeration<SearchResult> results = initialLdapContext
					.search(searchBase, searchFilter, searchControls);

			ArrayList<Role> res = new ArrayList<Role>();
			while (results.hasMoreElements()) {
				SearchResult searchResult = results.next();
				Attributes attrs = searchResult.getAttributes();
				LdifUser role;
				if (attrs.get("objectClass").contains("groupOfNames"))
					role = new LdifGroup(this, toDn(searchBase, searchResult),
							attrs);
				else if (attrs.get("objectClass").contains("inetOrgPerson"))
					role = new LdifUser(this, toDn(searchBase, searchResult),
							attrs);
				else
					throw new UserDirectoryException(
							"Unsupported LDAP type for "
									+ searchResult.getName());
				res.add(role);
			}
			return res.toArray(new Role[res.size()]);
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot get roles for filter "
					+ filter, e);
		}
	}

	@Override
	public User getUser(String key, String value) {
		if (key == null) {
			List<User> users = new ArrayList<User>();
			for (String prop : getIndexedUserProperties()) {
				User user = getUser(prop, value);
				if (user != null)
					users.add(user);
			}
			if (users.size() == 1)
				return users.get(0);
			else
				return null;
		}

		try {
			String searchFilter = "(&(objectClass=inetOrgPerson)(" + key + "="
					+ value + "))";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String searchBase = baseDn;
			NamingEnumeration<SearchResult> results = initialLdapContext
					.search(searchBase, searchFilter, searchControls);

			SearchResult searchResult = null;
			if (results.hasMoreElements()) {
				searchResult = (SearchResult) results.nextElement();
				if (results.hasMoreElements())
					searchResult = null;
			}
			if (searchResult == null)
				return null;
			return new LdifUser(this, toDn(searchBase, searchResult),
					searchResult.getAttributes());
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot get user with " + key
					+ "=" + value, e);
		}
	}

	@Override
	public Authorization getAuthorization(User user) {
		LdifUser u = (LdifUser) user;
		// populateDirectMemberOf(u);
		return new LdifAuthorization(u, getAllRoles(u));
	}

	private LdapName toDn(String baseDn, Binding binding)
			throws InvalidNameException {
		return new LdapName(binding.isRelative() ? binding.getName() + ","
				+ baseDn : binding.getName());
	}

	// void populateDirectMemberOf(LdifUser user) {
	//
	// try {
	// String searchFilter = "(&(objectClass=groupOfNames)(member="
	// + user.getName() + "))";
	//
	// SearchControls searchControls = new SearchControls();
	// searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	//
	// String searchBase = "ou=node";
	// NamingEnumeration<SearchResult> results = initialLdapContext
	// .search(searchBase, searchFilter, searchControls);
	//
	// // TODO synchro
	// //user.directMemberOf.clear();
	// while (results.hasMoreElements()) {
	// SearchResult searchResult = (SearchResult) results
	// .nextElement();
	// LdifGroup group = new LdifGroup(toDn(searchBase, searchResult),
	// searchResult.getAttributes());
	// populateDirectMemberOf(group);
	// //user.directMemberOf.add(group);
	// }
	// } catch (Exception e) {
	// throw new ArgeoException("Cannot populate direct members of "
	// + user, e);
	// }
	// }

	@Override
	protected List<? extends Group> getDirectGroups(User user) {
		List<Group> directGroups = new ArrayList<Group>();
		try {
			String searchFilter = "(&(objectClass=groupOfNames)(member="
					+ user.getName() + "))";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String searchBase = getGroupsSearchBase();
			NamingEnumeration<SearchResult> results = initialLdapContext
					.search(searchBase, searchFilter, searchControls);

			while (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results
						.nextElement();
				LdifGroup group = new LdifGroup(this, toDn(searchBase,
						searchResult), searchResult.getAttributes());
				directGroups.add(group);
			}
			return directGroups;
		} catch (Exception e) {
			throw new ArgeoException("Cannot populate direct members of "
					+ user, e);
		}
	}

	protected String getGroupsSearchBase() {
		// TODO configure group search base
		return baseDn;
	}
}
