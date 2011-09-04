package org.argeo.security.ui.rap;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** Configure Equinox login context from the bundle context. */
public class SecureRapActivator implements BundleActivator {

	public final static String ID = "org.argeo.security.ui.rap";
	public final static String CONTEXT_SPRING = "SPRING";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private static BundleContext bundleContext;

	public void start(BundleContext bundleContext) throws Exception {
		SecureRapActivator.bundleContext = bundleContext;
	}

	public void stop(BundleContext context) throws Exception {
	}

	static ILoginContext createLoginContext() {
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(CONTEXT_SPRING, configUrl);
	}
}
