package org.argeo.api.cms;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** A running node process. */
public interface CmsState {
	/** Local host on which this state is running. */
	String getHostname();

	/** Since when this state has been available. */
	Long getAvailableSince();

	UUID getUuid();

	/** A deploy property, or <code>null</code> if it is not set. */
	String getDeployProperty(String property);

	/**
	 * A list of size of the max count for this property, with null values when the
	 * property is not set, or an empty list (size 0) if this property is unknown.
	 */
	List<String> getDeployProperties(String property);

	/** A local path in the data area. */
	Path getDataPath(String relativePath);

	/** A local path in the state area. */
	Path getStatePath(String relativePath);
}
