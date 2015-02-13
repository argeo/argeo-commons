package org.argeo.security.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.osgi.framework.BundleContext;

/**
 * {@link CallbackHandler} that simply wraps a {@link BundleContext} and inject
 * it in provided {@link BundleContextCallback}
 */
public class BundleContextCallbackHandler implements CallbackHandler {
	private BundleContext bundleContext;

	public BundleContextCallbackHandler() {
	}

	public BundleContextCallbackHandler(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof BundleContextCallback)
				((BundleContextCallback) callback)
						.setBundleContext(bundleContext);
		}

	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
