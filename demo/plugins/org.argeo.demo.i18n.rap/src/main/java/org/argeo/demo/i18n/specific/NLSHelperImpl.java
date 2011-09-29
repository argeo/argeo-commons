package org.argeo.demo.i18n.specific;

import org.argeo.demo.i18n.NLSHelper;
import org.eclipse.rwt.RWT;

/**
 * Implements access to internationalized property using the RAP specific
 * implementation of NLS. Thanks to {@link http
 * ://eclipsesource.com/en/info/rcp-rap-single-sourcing-guideline/}
 */
public class NLSHelperImpl extends NLSHelper {

	protected Object internalGetMessages(String bundleName,
			@SuppressWarnings("rawtypes") Class clazz) {
		return RWT.NLS.getUTF8Encoded(bundleName, clazz);
	}
}
