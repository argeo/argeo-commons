package org.argeo.api.cms.directory;

import org.osgi.service.useradmin.Authorization;

/** An authorisation to a CMS system. */
public interface CmsAuthorization extends Authorization {
	/** The role which did imply this role, <code>null</code> if a direct role. */
	default String getImplyingRole(String role) {
		return null;
	}
}
