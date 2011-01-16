package org.argeo.security.equinox;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EquinoxSecurity implements BundleActivator {
	public final static String CONTEXT_SPRING = "SPRING";
	private static final String JAAS_CONFIG_FILE = "jaas/jaas_default.txt";

	private static BundleContext bundleContext;

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		// URL url = new URL(
		// "file:////home/mbaudier/dev/src/commons/security/eclipse/plugins/org.argeo.security.ui.rcp/jaas_config.txt");
		// // URL url = new URL(
		// //
		// "file:////home/mbaudier/dev/src/commons/security/eclipse/plugins/org.argeo.security.ui.rcp/jaas_config.txt");
		// ILoginContext secureContext = LoginContextFactory.createContext(
		// configName, url);
		getLoginContext();
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	static ILoginContext getLoginContext() {
		String configName = CONTEXT_SPRING;
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(configName, configUrl);
	}

}
