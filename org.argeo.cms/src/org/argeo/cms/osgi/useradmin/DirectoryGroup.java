package org.argeo.cms.osgi.useradmin;

import org.osgi.service.useradmin.Group;

/** A group in a user directroy. */
interface DirectoryGroup extends Group, DirectoryUser {
//	List<LdapName> getMemberNames();
}
