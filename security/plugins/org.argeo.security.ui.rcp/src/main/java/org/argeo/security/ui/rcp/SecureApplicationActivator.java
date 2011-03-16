package org.argeo.security.ui.rcp;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SecureApplicationActivator implements BundleActivator {

	public final static String CONTEXT_SPRING = "SPRING";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private static ILoginContext loginContext = null;

	public void start(BundleContext bundleContext) throws Exception {
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
