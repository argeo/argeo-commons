package org.argeo.cms.directory.ldap;

import static org.argeo.api.acr.ldap.LdapAttrs.objectClass;

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
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.api.acr.ldap.LdapAttrs;
import org.argeo.api.acr.ldap.LdapObjs;
import org.argeo.api.cms.directory.HierarchyUnit;

/** A user admin based on a LDAP server. */
public class LdapDao extends AbstractLdapDirectoryDao {
	private LdapConnection ldapConnection;

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

	@Override
	public boolean checkConnection() {
		try {
			return ldapConnection.entryExists(getDirectory().getBaseDn());
		} catch (NamingException e) {
			return false;
		}
	}

	@Override
	public boolean entryExists(LdapName dn) {
		try {
			return ldapConnection.entryExists(dn);
		} catch (NameNotFoundException e) {
			return false;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot check " + dn, e);
		}
	}

	@Override
	public LdapEntry doGetEntry(LdapName name) throws NameNotFoundException {
//		if (!entryExists(name))
//			throw new NameNotFoundException(name + " was not found in " + getDirectory().getBaseDn());
		try {
			Attributes attrs = ldapConnection.getAttributes(name);

			LdapEntry res;
			Rdn technicalRdn = LdapNameUtils.getParentRdn(name);
			if (getDirectory().getGroupBaseRdn().equals(technicalRdn)) {
				if (attrs.size() == 0) {// exists but not accessible
					attrs = new BasicAttributes();
					attrs.put(LdapAttrs.objectClass.name(), LdapObjs.top.name());
					attrs.put(LdapAttrs.objectClass.name(), getDirectory().getGroupObjectClass());
				}
				res = newGroup(name);
			} else if (getDirectory().getSystemRoleBaseRdn().equals(technicalRdn)) {
				if (attrs.size() == 0) {// exists but not accessible
					attrs = new BasicAttributes();
					attrs.put(LdapAttrs.objectClass.name(), LdapObjs.top.name());
					attrs.put(LdapAttrs.objectClass.name(), getDirectory().getGroupObjectClass());
				}
				res = newGroup(name);
			} else if (getDirectory().getUserBaseRdn().equals(technicalRdn)) {
				if (attrs.size() == 0) {// exists but not accessible
					attrs = new BasicAttributes();
					attrs.put(LdapAttrs.objectClass.name(), LdapObjs.top.name());
					attrs.put(LdapAttrs.objectClass.name(), getDirectory().getUserObjectClass());
				}
				res = newUser(name);
			} else {
				res = new DefaultLdapEntry(getDirectory(), name);
			}
			return res;
		} catch (NameNotFoundException e) {
			throw e;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot retrieve entry " + name, e);
		}
	}

	@Override
	public Attributes doGetAttributes(LdapName name) {
		try {
			Attributes attrs = ldapConnection.getAttributes(name);
			return attrs;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get attributes for " + name);
		}
	}

	@Override
	public List<LdapEntry> doGetEntries(LdapName searchBase, String f, boolean deep) {
		ArrayList<LdapEntry> res = new ArrayList<>();
		try {
			String searchFilter = f != null ? f.toString()
					: "(|(" + objectClass.name() + "=" + getDirectory().getUserObjectClass() + ")(" + objectClass.name()
							+ "=" + getDirectory().getGroupObjectClass() + "))";
			SearchControls searchControls = new SearchControls();
			// only attribute needed is objectClass
			searchControls.setReturningAttributes(new String[] { objectClass.name() });
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
					role = newGroup(dn);
				else if (objectClassAttr.contains(getDirectory().getUserObjectClass())
						|| objectClassAttr.contains(getDirectory().getUserObjectClass().toLowerCase()))
					role = newUser(dn);
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
			String structuralFilter = functionalOnly ? ""
					: "(" + getDirectory().getUserBaseRdn() + ")(" + getDirectory().getGroupBaseRdn() + ")("
							+ getDirectory().getSystemRoleBaseRdn() + ")";
			String searchFilter = "(|(" + objectClass + "=" + LdapObjs.organizationalUnit.name() + ")(" + objectClass
					+ "=" + LdapObjs.organization.name() + ")" + structuralFilter + ")";

			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			// no attributes needed
			searchControls.setReturningAttributes(new String[0]);

			NamingEnumeration<SearchResult> results = ldapConnection.search(searchBase, searchFilter, searchControls);

			while (results.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) results.nextElement();
				LdapName dn = toDn(searchBase, searchResult);
//				Attributes attrs = searchResult.getAttributes();
				LdapHierarchyUnit hierarchyUnit = new LdapHierarchyUnit(getDirectory(), dn);
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
			if (!ldapConnection.entryExists(dn))
				return null;
			return new LdapHierarchyUnit(getDirectory(), dn);
		} catch (NameNotFoundException e) {
			return null;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get hierarchy unit " + dn, e);
		}
	}

}
