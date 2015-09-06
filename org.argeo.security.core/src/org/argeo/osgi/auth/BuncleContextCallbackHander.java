package org.argeo.osgi.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.osgi.framework.BundleContext;

public class BuncleContextCallbackHander implements CallbackHandler {
	private final BundleContext bundleContext;

	public BuncleContextCallbackHander(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (!(callback instanceof BundleContextCallback))
				throw new UnsupportedCallbackException(callback);
			((BundleContextCallback) callback).setBundleContext(bundleContext);
		}

	}

}
