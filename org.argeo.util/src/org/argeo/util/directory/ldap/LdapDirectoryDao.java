package org.argeo.util.directory.ldap;

import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.transaction.WorkingCopyProcessor;

public interface LdapDirectoryDao extends WorkingCopyProcessor<LdapEntryWorkingCopy> {
	Boolean daoHasEntry(LdapName dn);

	LdapEntry daoGetEntry(LdapName name) throws NameNotFoundException;

	List<LdapEntry> doGetEntries(LdapName searchBase, String filter, boolean deep);

	List<LdapName> getDirectGroups(LdapName dn);

	Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly);

	HierarchyUnit doGetHierarchyUnit(LdapName dn);

	LdapEntry newUser(LdapName name, Attributes attrs);

	LdapEntry newGroup(LdapName name, Attributes attrs);
	
	void init();
	
	void destroy();
}
