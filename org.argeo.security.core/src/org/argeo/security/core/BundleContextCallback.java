package org.argeo.security.core;

import javax.security.auth.callback.Callback;

import org.osgi.framework.BundleContext;

/** Gives access to the OSGi {@link BundleContext} */
public class BundleContextCallback implements Callback {
	private BundleContext bundleContext;

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
