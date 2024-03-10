package org.argeo.api.cms.directory;

import java.util.Dictionary;

/** Parent of user/group hierarchy */
public interface CmsRole {
	String getName();

	// TODO replace with Map or ACR content
	@Deprecated
	Dictionary<String, Object> getProperties();
}
