package org.argeo.security.ui.rap;

import java.net.URL;

import org.argeo.ArgeoException;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SecureRapActivator implements BundleActivator {

	public final static String ID = "org.argeo.security.ui.rap";
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
		if (loginContext == null)
			throw new ArgeoException(
					"No Equinox login context available, check your configuration");
		return loginContext;
	}
}
