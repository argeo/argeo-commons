package org.argeo.cms.osgi.useradmin;

import org.argeo.api.cms.directory.CmsRole;
import org.osgi.service.useradmin.Role;

/** Merging interface between CMS and OSGi user management APIs. */
interface CmsOsgiRole extends CmsRole, Role {

}
