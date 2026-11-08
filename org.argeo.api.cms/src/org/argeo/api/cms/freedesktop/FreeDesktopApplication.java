package org.argeo.api.cms.freedesktop;

/**
 * The org.freedesktop.Application interface.
 */
public interface FreeDesktopApplication {
	String getObjectPath();

	void activate();

	void open(String[] uris);

	void activateAction(String actionName, String[] parameters);

}