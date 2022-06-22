package org.argeo.osgi.useradmin;

import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.transaction.xa.XAResource;

/** {@link XAResource} for a user directory being edited. */
class UserDirectoryWorkingCopy {
	// private final static Log log = LogFactory
	// .getLog(UserDirectoryWorkingCopy.class);

	private Map<LdapName, DirectoryUser> newUsers = new HashMap<LdapName, DirectoryUser>();
	private Map<LdapName, Attributes> modifiedUsers = new HashMap<LdapName, Attributes>();
	private Map<LdapName, DirectoryUser> deletedUsers = new HashMap<LdapName, DirectoryUser>();

	void cleanUp() {
		// clean collections
		newUsers.clear();
		newUsers = null;
		modifiedUsers.clear();
		modifiedUsers = null;
		deletedUsers.clear();
		deletedUsers = null;
	}

	public boolean noModifications() {
		return newUsers.size() == 0 && modifiedUsers.size() == 0 && deletedUsers.size() == 0;
	}

	public Attributes getAttributes(LdapName dn) {
		if (modifiedUsers.containsKey(dn))
			return modifiedUsers.get(dn);
		return null;
	}

	public void startEditing(DirectoryUser user) {
		LdapName dn = user.getDn();
		if (modifiedUsers.containsKey(dn))
			throw new IllegalStateException("Already editing " + dn);
		modifiedUsers.put(dn, (Attributes) user.getAttributes().clone());
	}

	public Map<LdapName, DirectoryUser> getNewUsers() {
		return newUsers;
	}

	public Map<LdapName, DirectoryUser> getDeletedUsers() {
		return deletedUsers;
	}

	public Map<LdapName, Attributes> getModifiedUsers() {
		return modifiedUsers;
	}
}
