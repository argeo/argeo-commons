package org.argeo.geotools;

import org.geotools.data.DataStore;

/** Constants used by the GeoTools utilities. */
public interface GeoToolsConstants {
	/**
	 * Property used to bastract the identification of some objects (typically
	 * {@link DataStore}. Especially useful as service property in OSGi.
	 */
	public final static String ALIAS_KEY = "alias";
}
