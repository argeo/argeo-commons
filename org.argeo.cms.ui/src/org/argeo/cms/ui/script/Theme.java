package org.argeo.cms.ui.script;

import org.argeo.cms.ui.util.CmsTheme;
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