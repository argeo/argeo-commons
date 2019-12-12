package org.argeo.osgi.boot.equinox;

import java.util.Map;

import org.argeo.osgi.boot.OsgiBootUtils;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.launch.Framework;

/**
 * Utilities with a dependency to the Equinox OSGi runtime or its configuration.
 */
public class EquinoxUtils {

	public static Framework launch(Map<String, String> configuration) {
		return OsgiBootUtils.launch(new EquinoxFactory(), configuration);
	}

	/** Singleton. */
	private EquinoxUtils() {

	}
}
