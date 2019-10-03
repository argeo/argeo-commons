package org.argeo.cms.script;

import org.argeo.cms.util.CmsTheme;
import org.osgi.framework.BundleContext;

/** @deprecated Use <code>CmsTheme</code> instead. */
@Deprecated
public class Theme extends CmsTheme {

	public Theme(BundleContext bundleContext, String symbolicName) {
		super(bundleContext, symbolicName);
	}

	public Theme(BundleContext bundleContext) {
		super(bundleContext);
	}

}
