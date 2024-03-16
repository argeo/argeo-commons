package org.argeo.cms.osgi.useradmin;

import org.argeo.api.cms.directory.CmsAuthorization;
import org.osgi.service.useradmin.Authorization;

/** Merging interface between CMS and OSGi user management APIs. */
interface CmsOsgiAuthorization extends CmsAuthorization, Authorization {

}
