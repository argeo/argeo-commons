package org.argeo.security.ui.rcp;

import java.io.IOException;
import java.net.URL;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.argeo.security.ui.dialogs.DefaultLoginDialog;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/** Activator able to create {@link ILoginContext} */
public class SecureApplicationActivator implements BundleActivator {

	public final static String CONTEXT_NIX = "NIX";
	public final static String CONTEXT_WINDOWS = "WINDOWS";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private static BundleContext bundleContext;
	private ServiceRegistration callbackHandlerRegistration;

	public void start(BundleContext bundleContext) throws Exception {
		SecureApplicationActivator.bundleContext = bundleContext;

		CallbackHandler callbackHandler = new CallbackHandler() {

			public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException {
				DefaultLoginDialog dialog = new DefaultLoginDialog();
				dialog.handle(callbacks);
			}
		};
		callbackHandlerRegistration = bundleContext.registerService(
				CallbackHandler.class.getName(), callbackHandler, null);
	}

	public void stop(BundleContext context) throws Exception {
		callbackHandlerRegistration.unregister();
	}

	static ILoginContext createLoginContext(String context) {
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(context, configUrl);
	}
}
