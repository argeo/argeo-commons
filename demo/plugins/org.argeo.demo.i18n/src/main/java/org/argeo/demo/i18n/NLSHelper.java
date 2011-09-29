package org.argeo.demo.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * thanks to {@link http
 * ://eclipsesource.com/en/info/rcp-rap-single-sourcing-guideline/}
 */
public abstract class NLSHelper {
	private final static NLSHelper IMPL;

	static {
		IMPL = (NLSHelper) ImplementationLoader.newInstance(NLSHelper.class);
	}

	public static NLS getMessages(String bundleName,
			@SuppressWarnings("rawtypes") Class clazz) {
		return (NLS) IMPL.internalGetMessages(bundleName, clazz);
	}

	protected abstract Object internalGetMessages(String bundleName,
			@SuppressWarnings("rawtypes") Class clazz);
}
