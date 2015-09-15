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
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.osgi.framework.Filter;
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
	protected Boolean daoHasRole(LdapName dn) {
		return daoGetRole(dn) != null;
	}

	@Override
	protected DirectoryUser daoGetRole(LdapName name) {
		try {
			Attributes attrs = initialLdapContext.getAttributes(name);
			if (attrs.size() == 0)
				return null;
			LdifUser res;
			if (attrs.get("objectClass").contains("groupOfNames"))
				res = new LdifGroup(this, name, attrs);
			else if (attrs.get("objectClass").contains("inetOrgPerson"))
				res = new LdifUser(this, name, attrs);
			else
				throw new UserDirectoryException("Unsupported LDAP type for "
						+ name);
			return res;
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot get role for " + name, e);
		}
	}

	@Override
	protected List<DirectoryUser> doGetRoles(Filter f) {
		// TODO Auto-generated method stub
		try {
			String searchFilter = f != null ? f.toString()
					: "(|(objectClass=inetOrgPerson)(objectClass=groupOfNames))";
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String searchBase = baseDn;
			NamingEnumeration<SearchResult> results = initialLdapContext
					.search(searchBase, searchFilter, searchControls);

			ArrayList<DirectoryUser> res = new ArrayList<DirectoryUser>();
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
			return res;
		} catch (Exception e) {
			throw new UserDirectoryException(
					"Cannot get roles for filter " + f, e);
		}
	}

	@Override
	protected void doGetUser(String key, String value,
			List<DirectoryUser> collectedUsers) {
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
			if (searchResult != null)
				collectedUsers.add(new LdifUser(this, toDn(searchBase,
						searchResult), searchResult.getAttributes()));
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot get user with " + key
					+ "=" + value, e);
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
	protected List<DirectoryGroup> getDirectGroups(User user) {
		List<DirectoryGroup> directGroups = new ArrayList<DirectoryGroup>();
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

	@Override
	protected void prepare(WorkingCopy wc) {
		try {
			initialLdapContext.reconnect(initialLdapContext
					.getConnectControls());
			// delete
			for (LdapName dn : wc.getDeletedUsers().keySet()) {
				if (!entryExists(dn))
					throw new UserDirectoryException("User to delete no found "
							+ dn);
			}
			// add
			for (LdapName dn : wc.getNewUsers().keySet()) {
				if (!entryExists(dn))
					throw new UserDirectoryException("User to create found "
							+ dn);
			}
			// modify
			for (LdapName dn : wc.getModifiedUsers().keySet()) {
				if (!entryExists(dn))
					throw new UserDirectoryException("User to modify no found "
							+ dn);
			}
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot prepare LDAP", e);
		}
	}

	private boolean entryExists(LdapName dn) throws NamingException {
		return initialLdapContext.getAttributes(dn).size() != 0;
	}

	@Override
	protected void commit(WorkingCopy wc) {
		try {
			// delete
			for (LdapName dn : wc.getDeletedUsers().keySet()) {
				initialLdapContext.destroySubcontext(dn);
			}
			// add
			for (LdapName dn : wc.getNewUsers().keySet()) {
				DirectoryUser user = wc.getNewUsers().get(dn);
				initialLdapContext.createSubcontext(dn, user.getAttributes());
			}
			// modify
			for (LdapName dn : wc.getModifiedUsers().keySet()) {
				Attributes modifiedAttrs = wc.getModifiedUsers().get(dn);
				initialLdapContext.modifyAttributes(dn,
						DirContext.REPLACE_ATTRIBUTE, modifiedAttrs);
			}
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot commit LDAP", e);
		}
	}

	@Override
	protected void rollback(WorkingCopy wc) {
		// TODO Auto-generated method stub
		super.rollback(wc);
	}

}
