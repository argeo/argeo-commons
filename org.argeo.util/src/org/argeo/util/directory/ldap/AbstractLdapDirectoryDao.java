package org.argeo.util.directory.ldap;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

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
	public LdapEntry newUser(LdapName name, Attributes attrs) {
		return getDirectory().newUser(name, attrs);
	}

	@Override
	public LdapEntry newGroup(LdapName name, Attributes attrs) {
		return getDirectory().newGroup(name, attrs);
	}

}
