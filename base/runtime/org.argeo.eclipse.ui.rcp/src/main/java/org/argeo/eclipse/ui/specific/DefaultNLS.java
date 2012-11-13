package org.argeo.eclipse.ui.specific;

import org.eclipse.osgi.util.NLS;

/** RCP specific {@link NLS} to be extended */
public class DefaultNLS extends NLS {
	public final static String DEFAULT_BUNDLE_LOCATION = "/properties/plugin";

	public DefaultNLS() {
		this(DEFAULT_BUNDLE_LOCATION);
	}

	public DefaultNLS(String bundleName) {
		NLS.initializeMessages(bundleName, getClass());
	}
}
