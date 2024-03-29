package org.argeo.cms.directory.ldap;

import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.transaction.WorkingCopyProcessor;

/** Low-level access to an LDAP/LDIF directory. */
public interface LdapDirectoryDao extends WorkingCopyProcessor<LdapEntryWorkingCopy> {
	boolean checkConnection();

	boolean entryExists(LdapName dn);

	LdapEntry doGetEntry(LdapName name) throws NameNotFoundException;

	Attributes doGetAttributes(LdapName name);

	List<LdapEntry> doGetEntries(LdapName searchBase, String filter, boolean deep);

	List<LdapName> getDirectGroups(LdapName dn);

	Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly);

	HierarchyUnit doGetHierarchyUnit(LdapName dn);

	LdapEntry newUser(LdapName name);

	LdapEntry newGroup(LdapName name);

	void init();

	void destroy();
}
