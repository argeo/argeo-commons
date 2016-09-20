package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.LdifName.objectClass;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Filter;

/**
 * A user admin based on a LDAP server. Requires a {@link TransactionManager}
 * and an open transaction for write access.
 */
public class LdapUserAdmin extends AbstractUserDirectory {
	private final static Log log = LogFactory.getLog(LdapUserAdmin.class);

	private InitialLdapContext initialLdapContext = null;

	public LdapUserAdmin(Dictionary<String, ?> properties) {
		super(properties);
		try {
			Hashtable<String, Object> connEnv = new Hashtable<String, Object>();
			connEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			connEnv.put(Context.PROVIDER_URL, getUri().toString());
			connEnv.put("java.naming.ldap.attributes.binary", LdifName.userPassword.name());

			initialLdapContext = new InitialLdapContext(connEnv, null);
			// StartTlsResponse tls = (StartTlsResponse) ctx
			// .extendedOperation(new StartTlsRequest());
			// tls.negotiate();
			initialLdapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
			Object principal = properties.get(Context.SECURITY_PRINCIPAL);
			if (principal != null) {
				initialLdapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, principal.toString());
				Object creds = properties.get(Context.SECURITY_CREDENTIALS);
				if (creds != null) {
					initialLdapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, creds.toString());

				}
			}
			// initialLdapContext.addToEnvironment(Context.SECURITY_PRINCIPAL,
			// "uid=admin,ou=system");
			// initialLdapContext.addToEnvironment(Context.SECURITY_CREDENTIALS,
			// "secret");
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

	protected InitialLdapContext getLdapContext() {
		return initialLdapContext;
	}

	@Override
	protected Boolean daoHasRole(LdapName dn) {
		return daoGetRole(dn) != null;
	}

	@Override
	protected DirectoryUser daoGetRole(LdapName name) {
		try {
			Attributes attrs = getLdapContext().getAttributes(name);
			if (attrs.size() == 0)
				return null;
			LdifUser res;
			if (attrs.get(objectClass.name()).contains(getGroupObjectClass()))
				res = new LdifGroup(this, name, attrs);
			else if (attrs.get(objectClass.name()).contains(getUserObjectClass()))
				res = new LdifUser(this, name, attrs);
			else
				throw new UserDirectoryException("Unsupported LDAP type for " + name);
			return res;
		} catch (NamingException e) {
			return null;
		}
	}

	@Override
	protected List<DirectoryUser> doGetRoles(Filter f) {
		try {
			String searchFilter = f != null ? f.toString()
					: "(|(" + objectClass + "=" + getUserObjectClass() + ")(" + objectClass + "="
							+ getGroupObjectClass() + "))";
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			LdapName searchBase = getBaseDn();
			NamingEnumeration<SearchResult> results = getLdapContext().search(searchBase, searchFilter, searchControls);

			ArrayList<DirectoryUser> res = new ArrayList<DirectoryUser>();
			results: while (results.hasMoreElements()) {
				SearchResult searchResult = results.next();
				Attributes attrs = searchResult.getAttributes();
				Attribute objectClassAttr = attrs.get(objectClass.name());
				LdapName dn = toDn(searchBase, searchResult);
				LdifUser role;
				if (objectClassAttr.contains(getGroupObjectClass()))
					role = new LdifGroup(this, dn, attrs);
				else if (objectClassAttr.contains(getUserObjectClass()))
					role = new LdifUser(this, dn, attrs);
				else {
					log.warn("Unsupported LDAP type for " + searchResult.getName());
					continue results;
				}
				res.add(role);
			}
			return res;
		} catch (Exception e) {
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
			NamingEnumeration<SearchResult> results = getLdapContext().search(searchBase, searchFilter, searchControls);

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
			getLdapContext().reconnect(getLdapContext().getConnectControls());
			// delete
			for (LdapName dn : wc.getDeletedUsers().keySet()) {
				if (!entryExists(dn))
					throw new UserDirectoryException("User to delete no found " + dn);
			}
			// add
			for (LdapName dn : wc.getNewUsers().keySet()) {
				if (entryExists(dn))
					throw new UserDirectoryException("User to create found " + dn);
			}
			// modify
			for (LdapName dn : wc.getModifiedUsers().keySet()) {
				if (!wc.getNewUsers().containsKey(dn) && !entryExists(dn))
					throw new UserDirectoryException("User to modify not found " + dn);
			}
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot prepare LDAP", e);
		}
	}

	private boolean entryExists(LdapName dn) throws NamingException {
		try {
			return getLdapContext().getAttributes(dn).size() != 0;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	@Override
	protected void commit(UserDirectoryWorkingCopy wc) {
		try {
			// delete
			for (LdapName dn : wc.getDeletedUsers().keySet()) {
				getLdapContext().destroySubcontext(dn);
			}
			// add
			for (LdapName dn : wc.getNewUsers().keySet()) {
				DirectoryUser user = wc.getNewUsers().get(dn);
				getLdapContext().createSubcontext(dn, user.getAttributes());
			}
			// modify
			for (LdapName dn : wc.getModifiedUsers().keySet()) {
				Attributes modifiedAttrs = wc.getModifiedUsers().get(dn);
				getLdapContext().modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, modifiedAttrs);
			}
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot commit LDAP", e);
		}
	}

	@Override
	protected void rollback(UserDirectoryWorkingCopy wc) {
		// prepare not impacting
	}

}