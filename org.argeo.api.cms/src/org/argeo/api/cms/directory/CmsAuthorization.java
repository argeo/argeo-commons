package org.argeo.api.cms.directory;

/** An authorisation to a CMS system. */
public interface CmsAuthorization {
	/** The role which did imply this role, <code>null</code> if a direct role. */
	default String getImplyingRole(String role) {
		return null;
	}
}
