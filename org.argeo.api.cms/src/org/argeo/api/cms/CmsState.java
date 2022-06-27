package org.argeo.api.cms;

import java.nio.file.Path;
import java.util.UUID;

/** A running node process. */
public interface CmsState {
	String getHostname();

	Long getAvailableSince();

	UUID getUuid();

	String getDeployProperty(String key);
	
	Path getDataPath(String relativePath);
}
