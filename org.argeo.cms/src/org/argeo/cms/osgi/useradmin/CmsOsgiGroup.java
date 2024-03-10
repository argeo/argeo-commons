package org.argeo.cms.osgi.useradmin;

import org.argeo.api.cms.directory.CmsGroup;
import org.osgi.service.useradmin.Group;

/** Merging interface between CMS and OSGi user management APIs. */
interface CmsOsgiGroup extends CmsOsgiUser, CmsGroup, Group {

}
