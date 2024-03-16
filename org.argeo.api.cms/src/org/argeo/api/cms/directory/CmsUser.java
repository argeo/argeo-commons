package org.argeo.api.cms.directory;

/**
 * An entity with credentials which can log in to a system. Can be a real person
 * or not.
 */
public interface CmsUser extends CmsRole {
	String getDisplayName();
}
