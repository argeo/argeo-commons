package org.argeo.cms.directory.ldap;

import javax.naming.ldap.LdapName;

/** Base class for LDAP/LDIF directory DAOs. */
public abstract class AbstractLdapDirectoryDao implements LdapDirectoryDao {

	private AbstractLdapDirectory directory;

	public AbstractLdapDirectoryDao(AbstractLdapDirectory directory) {
		this.directory = directory;
	}

	public AbstractLdapDirectory getDirectory() {
		return directory;
	}

	@Override
	public LdapEntryWorkingCopy newWorkingCopy() {
		return new LdapEntryWorkingCopy();
	}

	@Override
	public LdapEntry newUser(LdapName name) {
		return getDirectory().newUser(name);
	}

	@Override
	public LdapEntry newGroup(LdapName name) {
		return getDirectory().newGroup(name);
	}

}
