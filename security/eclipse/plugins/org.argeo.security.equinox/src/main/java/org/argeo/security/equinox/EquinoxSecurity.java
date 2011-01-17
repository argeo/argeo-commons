package org.argeo.security.equinox;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EquinoxSecurity implements BundleActivator {
	public final static String CONTEXT_SPRING = "SPRING";
	private static final String JAAS_CONFIG_FILE = "jaas/jaas_default.txt";

	private static ILoginContext loginContext = null;

	public void start(BundleContext bundleContext) throws Exception {
		// getLoginContext();
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		loginContext = LoginContextFactory.createContext(CONTEXT_SPRING,
				configUrl);
	}

	public void stop(BundleContext context) throws Exception {
	}

	static ILoginContext getLoginContext() {
		return loginContext;
	}

}
