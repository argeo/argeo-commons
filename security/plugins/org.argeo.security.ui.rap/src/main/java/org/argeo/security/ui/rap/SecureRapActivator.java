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
	public final static String CONTEXT_SPRING_ANONYMOUS = "SPRING_ANONYMOUS";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private BundleContext bundleContext;
	private static SecureRapActivator activator = null;

	public void start(BundleContext bundleContext) throws Exception {
		activator = this;
		this.bundleContext = bundleContext;
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		activator = null;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public static SecureRapActivator getActivator() {
		return activator;
	}

	static ILoginContext createLoginContext(String contextName) {
		URL configUrl = getActivator().getBundleContext().getBundle()
				.getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(contextName, configUrl);
	}
}
