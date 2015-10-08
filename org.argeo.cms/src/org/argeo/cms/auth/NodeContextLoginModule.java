package org.argeo.cms.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.cms.internal.kernel.Activator;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.Authorization;

/** Populates the shared state with this node context. */
public class NodeContextLoginModule implements LoginModule, AuthConstants {
	private Subject subject;
	private Map<String, Object> sharedState;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		sharedState.put(AuthConstants.BUNDLE_CONTEXT_KEY, Activator.getBundleContext());
		Display display = Display.getCurrent();
		if (display != null) {
			Authorization authorization = (Authorization) display
					.getData(AuthConstants.AUTHORIZATION_KEY);
			if (authorization != null)
				sharedState.put(AuthConstants.AUTHORIZATION_KEY, authorization);
		}
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		Display display = Display.getCurrent();
		if (display != null) {
			Authorization authorization = subject
					.getPrivateCredentials(Authorization.class).iterator()
					.next();
			display.setData(AuthConstants.AUTHORIZATION_KEY, authorization);
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		sharedState.remove(AuthConstants.BUNDLE_CONTEXT_KEY);
		sharedState.remove(AuthConstants.AUTHORIZATION_KEY);
		Display display = Display.getCurrent();
		if (display != null)
			display.setData(AuthConstants.AUTHORIZATION_KEY, null);
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		sharedState.remove(AuthConstants.BUNDLE_CONTEXT_KEY);
		sharedState.remove(AuthConstants.AUTHORIZATION_KEY);
		Display display = Display.getCurrent();
		if (display != null)
			display.setData(AuthConstants.AUTHORIZATION_KEY, null);
		return true;
	}

}
