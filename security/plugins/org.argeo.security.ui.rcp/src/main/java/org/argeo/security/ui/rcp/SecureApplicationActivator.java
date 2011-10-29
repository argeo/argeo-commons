package org.argeo.security.ui.rcp;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** Activator able to create {@link ILoginContext} */
public class SecureApplicationActivator implements BundleActivator {

	public final static String CONTEXT_REMOTE = "REMOTE";
	public final static String CONTEXT_NIX = "NIX";
	public final static String CONTEXT_WINDOWS = "WINDOWS";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private static BundleContext bundleContext;

	public void start(BundleContext bundleContext) throws Exception {
		SecureApplicationActivator.bundleContext = bundleContext;
	}

	public void stop(BundleContext context) throws Exception {
	}

	static ILoginContext createLoginContext(String context) {
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(context, configUrl);
	}
}
