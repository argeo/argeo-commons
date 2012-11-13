package org.argeo.eclipse.ui.specific;

import org.eclipse.osgi.util.NLS;
import org.eclipse.rwt.RWT;

/** NLS attached to a given thread */
public class ThreadNLS<T extends NLS> extends InheritableThreadLocal<T> {
	public final static String DEFAULT_BUNDLE_LOCATION = "/properties/plugin";

	private final String bundleLocation;

	private Class<T> type;
	private Boolean utf8 = false;

	public ThreadNLS(String bundleLocation, Class<T> type, Boolean utf8) {
		this.bundleLocation = bundleLocation;
		this.type = type;
		this.utf8 = utf8;
	}

	public ThreadNLS(Class<T> type) {
		this(DEFAULT_BUNDLE_LOCATION, type, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T initialValue() {
		if (utf8)
			return (T) RWT.NLS.getUTF8Encoded(bundleLocation, type);
		else
			return (T) RWT.NLS.getISO8859_1Encoded(bundleLocation, type);
	}
}
