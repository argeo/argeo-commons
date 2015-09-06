package org.argeo.osgi.auth;

import javax.security.auth.callback.Callback;
import javax.security.auth.spi.LoginModule;

import org.osgi.framework.BundleContext;

/** Allows a {@link LoginModule} to as for a {@link BundleContext} */
public class BundleContextCallback implements Callback {
	private BundleContext bundleContext;

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
