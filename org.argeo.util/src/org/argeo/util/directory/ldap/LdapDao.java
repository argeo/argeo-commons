package org.argeo.util.directory.ldap;

import static org.argeo.util.naming.LdapAttrs.objectClass;

import java.util.ArrayList;
import java.util.List;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Binding;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.naming.LdapObjs;

/** A user admin based on a LDAP server. */
public class LdapDao extends AbstractLdapDirectoryDao {
	private LdapConnection ldapConnection;

//	public LdapUserAdmin(Dictionary<String, ?> properties) {
//		this(properties, false);
//	}

	public LdapDao(AbstractLdapDirectory directory) {
		super(directory);
	}

	@Override
	public void init() {
		ldapConnection = new LdapConnection(getDirectory().getUri().toString(), getDirectory().cloneConfigProperties());
	}

	public void destroy() {
		ldapConnection.destroy();
	}

//	@Override
//	protected AbstractUserDirectory scope(User user) {
//		Dictionary<String, Object> credentials = user.getCredentials();
//		String username = (String) credentials.get(SHARED_STATE_USERNAME);
//		if (username == null)
//			username = user.getName();
//		Dictionary<String, Object> properties = cloneProperties();
//		properties.put(Context.SECURITY_PRINCIPAL, username.toString());
//		Object pwdCred = credentials.get(SHARED_STATE_PASSWORD);
//		byte[] pwd = (byte[]) pwdCred;
//		if (pwd != null) {
//			char[] password = DirectoryDigestUtils.bytesToChars(pwd);
//			properties.put(Context.SECURITY_CREDENTIALS, new String(password));
//		} else {
//			properties.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
//		}
//		return new LdapUserAdmin(properties, true);
//	}

//	protected InitialLdapContext getLdapContext() {
//		return initialLdapContext;
//	}

	@Override
	public Boolean entryExists(LdapName dn) {
		try {
			return doGetEntry(dn) != null;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	@Override
	public LdapEntry doGetEntry(LdapName name) throws NameNotFoundException {
		try {
			Attributes attrs = ldapConnection.getAttributes(name);
			if (attrs.size() == 0)
				return null;
//			int roleType = roleType(name);
			LdapEntry res;
			Rdn technicalRdn = LdapNameUtils.getParentRdn(name);
			if (getDirectory().getGroupBaseRdn().equals(technicalRdn))
				res = newGroup(name, attrs);
			else if (getDirectory().getSystemRoleBaseRdn().equals(technicalRdn))
				res = newGroup(name, attrs);
			else if (getDirectory().getUserBaseRdn().equals(technicalRdn))
				res = newUser(name, attrs);
			else
				res = new DefaultLdapEntry(getDirectory(), name, attrs);
//			if (isGroup(name))
//				res = newGroup(name, attrs);
//			else
//				res = newUser(name, attrs);
//			else
//				throw new IllegalArgumentException("Unsupported LDAP type for " + name);
			return res;
		} catch (NameNotFoundException e) {
			throw e;
		} catch (NamingException e) {
			return null;
		}
	}

//	protected boolean isGroup(LdapName dn) {
//		Rdn technicalRdn = LdapNameUtils.getParentRdn(dn);
//		if (getDirectory().getGroupBaseRdn().equals(technicalRdn)
//				|| getDirectory().getSystemRoleBaseRdn().equals(technicalRdn))
//			return true;
//		else if (getDirectory().getUserBaseRdn().equals(technicalRdn))
//			return false;
//		else
//			throw new IllegalArgumentException(
//					"Cannot find role type, " + technicalRdn + " is not a technical RDN for " + dn);
//	}

	@Override
	public List<LdapEntry> doGetEntries(LdapName searchBase, String f, boolean deep) {
		ArrayList<LdapEntry> res = new ArrayList<>();
		try {
			String searchFilter = f != null ? f.toString()
					: "(|(" + objectClass + "=" + getDirectory().getUserObjectClass() + ")(" + objectClass + "="
							+ getDirectory().getGroupObjectClass() + "))";
			SearchControls searchControls = new SearchControls();
			// FIXME make one level consistent with deep
			searchControls.setSearchScope(deep ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);

			// LdapName searchBase = getBaseDn();
			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			results: while (results.hasMoreElements()) {
				SearchResult searchResult = results.next();
				Attributes attrs = searchResult.getAttributes();
				Attribute objectClassAttr = attrs.get(objectClass.name());
				LdapName dn = toDn(searchBase, searchResult);
				LdapEntry role;
				if (objectClassAttr.contains(getDirectory().getGroupObjectClass())
						|| objectClassAttr.contains(getDirectory().getGroupObjectClass().toLowerCase()))
					role = newGroup(dn, attrs);
				else if (objectClassAttr.contains(getDirectory().getUserObjectClass())
						|| objectClassAttr.contains(getDirectory().getUserObjectClass().toLowerCase()))
					role = newUser(dn, attrs);
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
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get roles for filter " + f, e);
		}
	}

	private LdapName toDn(LdapName baseDn, Binding binding) throws InvalidNameException {
		return new LdapName(binding.isRelative() ? binding.getName() + "," + baseDn : binding.getName());
	}

	@Override
	public List<LdapName> getDirectGroups(LdapName dn) {
		List<LdapName> directGroups = new ArrayList<LdapName>();
		try {
			String searchFilter = "(&(" + objectClass + "=" + getDirectory().getGroupObjectClass() + ")("
					+ getDirectory().getMemberAttributeId() + "=" + dn + "))";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			LdapName searchBase = getDirectory().getBaseDn();
			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			while (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.nextElement();
				directGroups.add(toDn(searchBase, searchResult));
			}
			return directGroups;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot populate direct members of " + dn, e);
		}
	}

	@Override
	public void prepare(LdapEntryWorkingCopy wc) {
		try {
			ldapConnection.prepareChanges(wc);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot prepare LDAP", e);
		}
	}

	@Override
	public void commit(LdapEntryWorkingCopy wc) {
		try {
			ldapConnection.commitChanges(wc);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot commit LDAP", e);
		}
	}

	@Override
	public void rollback(LdapEntryWorkingCopy wc) {
		// prepare not impacting
	}

	/*
	 * HIERARCHY
	 */

	@Override
	public Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly) {
		List<HierarchyUnit> res = new ArrayList<>();
		try {
			String searchFilter = "(|(" + objectClass + "=" + LdapObjs.organizationalUnit.name() + ")(" + objectClass
					+ "=" + LdapObjs.organization.name() + "))";
//			String searchFilter = "(|(" + objectClass + "=" + LdapObjs.organizationalUnit.name() + ")(" + objectClass
//					+ "=" + LdapObjs.organization.name() + ")(cn=accounts)(cn=users)(cn=groups))";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);

			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			while (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.nextElement();
				LdapName dn = toDn(searchBase, searchResult);
				Attributes attrs = searchResult.getAttributes();
				LdapHierarchyUnit hierarchyUnit = new LdapHierarchyUnit(getDirectory(), dn, attrs);
				if (functionalOnly) {
					if (hierarchyUnit.isFunctional())
						res.add(hierarchyUnit);
				} else {
					res.add(hierarchyUnit);
				}
			}
			return res;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get direct hierarchy units ", e);
		}
	}

	@Override
	public HierarchyUnit doGetHierarchyUnit(LdapName dn) {
		try {
			if (getDirectory().getBaseDn().equals(dn))
				return getDirectory();
			if (!dn.startsWith(getDirectory().getBaseDn()))
				throw new IllegalArgumentException(dn + " does not start with base DN " + getDirectory().getBaseDn());
			Attributes attrs = ldapConnection.getAttributes(dn);
			return new LdapHierarchyUnit(getDirectory(), dn, attrs);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get hierarchy unit " + dn, e);
		}
	}

}
