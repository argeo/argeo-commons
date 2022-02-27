package org.argeo.api.cms;

/** A running node process. */
public interface CmsState {
	String getHostname();

	Long getAvailableSince();

}
