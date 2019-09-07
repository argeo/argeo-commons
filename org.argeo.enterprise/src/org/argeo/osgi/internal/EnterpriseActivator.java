package org.argeo.osgi.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Called to gather information about the OSGi runtime. Should not activate
 * anything else that canonical monitoring services (not creating implicit
 * APIs), which is the responsibility of higher levels..
 */
public class EnterpriseActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
