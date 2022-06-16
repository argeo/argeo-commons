package org.argeo.api.cms;

import java.util.UUID;

/** A running node process. */
public interface CmsState {
	String getHostname();

	Long getAvailableSince();

	UUID getUuid();
}
