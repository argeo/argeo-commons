package org.argeo.osgi.useradmin;

import org.argeo.util.directory.ldap.LdapEntry;
import org.osgi.service.useradmin.User;

/** A user in a user directory. */
interface DirectoryUser extends User, LdapEntry {
}
