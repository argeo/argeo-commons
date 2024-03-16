package org.argeo.api.cms.directory;

import java.util.Set;

/** A group in a user directory. */
public interface CmsGroup extends CmsUser {
	Set<? extends CmsRole> getDirectMembers();
}
