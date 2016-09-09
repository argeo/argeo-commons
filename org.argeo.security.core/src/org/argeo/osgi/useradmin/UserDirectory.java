package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;
import javax.transaction.xa.XAResource;

/** Information about a user directory. */
public interface UserDirectory {
	/** The base DN of all entries in this user directory */
	public LdapName getBaseDn();

	/** The related {@link XAResource} */
	public XAResource getXaResource();

	public boolean isReadOnly();

	public String getUserObjectClass();

	public String getUserBase();

	public String getGroupObjectClass();

	public String getGroupBase();
}
