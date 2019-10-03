package org.argeo.osgi.useradmin;

import javax.naming.ldap.LdapName;
import javax.transaction.xa.XAResource;

/** Information about a user directory. */
public interface UserDirectory {
	/** The base DN of all entries in this user directory */
	LdapName getBaseDn();

	/** The related {@link XAResource} */
	XAResource getXaResource();

	boolean isReadOnly();

	boolean isDisabled();

	String getUserObjectClass();

	String getUserBase();

	String getGroupObjectClass();

	String getGroupBase();
}
