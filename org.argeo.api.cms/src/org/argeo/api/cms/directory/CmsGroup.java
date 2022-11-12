package org.argeo.api.cms.directory;

import org.osgi.service.useradmin.Group;

/** A group in a user directroy. */
public interface CmsGroup extends Group, CmsUser {
//	List<LdapName> getMemberNames();
}
