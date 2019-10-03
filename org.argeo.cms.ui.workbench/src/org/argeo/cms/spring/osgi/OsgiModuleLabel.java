package org.argeo.cms.spring.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Logs the name and version of an OSGi bundle based on its
 * {@link BundleContext}.
 */
public class OsgiModuleLabel {
	private final static Log log = LogFactory.getLog(OsgiModuleLabel.class);

	private Bundle bundle;

	public OsgiModuleLabel() {
	}

	/** Sets without logging. */
	public OsgiModuleLabel(Bundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * Retrieved bundle from a bundle context and logs it. Typically to be set
	 * as a Spring bean.
	 */
	public void setBundleContext(BundleContext bundleContext) {
		this.bundle = bundleContext.getBundle();
		log.info(msg());
	}

	public String msg() {
		String name = bundle.getHeaders().get(Constants.BUNDLE_NAME).toString();
		String symbolicName = bundle.getSymbolicName();
		String version = bundle.getVersion().toString();
		return name + " v" + version + " (" + symbolicName + ")";
	}
}
