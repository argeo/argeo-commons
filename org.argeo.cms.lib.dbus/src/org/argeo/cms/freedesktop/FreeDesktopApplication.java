package org.argeo.cms.freedesktop;

import java.util.List;

/**
 * The org.freedesktop.Application interface.
 */
public interface FreeDesktopApplication {
	String getObjectPath();

	void activate();

	void open(List<String> uris);

	void activateAction(String actionName);
	// void activateAction(String actionName, List<Variant<?>> parameter);

}