package org.argeo.cms.osgi.useradmin;

import org.argeo.api.cms.directory.CmsUser;
import org.osgi.service.useradmin.User;

/** Merging interface between CMS and OSGi user management APIs. */
interface CmsOsgiUser extends CmsOsgiRole, CmsUser, User {

}
