package org.argeo.api.cms;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** A running node process. */
public interface CmsState {
	String getHostname();

	Long getAvailableSince();

	UUID getUuid();

	String getDeployProperty(String property);

	/**
	 * A list of size of the max count for this property, with null values when the
	 * property is not set, or an empty list (size 0) if this property is unknown.
	 */
	List<String> getDeployProperties(String property);

	Path getDataPath(String relativePath);

	Path getStatePath(String relativePath);
}
