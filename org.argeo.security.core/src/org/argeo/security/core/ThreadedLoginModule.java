package org.argeo.security.core;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/** Attach login modules to threads. */
public abstract class ThreadedLoginModule implements LoginModule {
	private ThreadLocal<LoginModule> loginModule = new ThreadLocal<LoginModule>() {

		@Override
		protected LoginModule initialValue() {
			return createLoginModule();
		}

	};

	protected abstract LoginModule createLoginModule();

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		loginModule.get().initialize(subject, callbackHandler, sharedState,
				options);
	}

	@Override
	public boolean login() throws LoginException {
		return loginModule.get().login();
	}

	@Override
	public boolean commit() throws LoginException {
		return loginModule.get().commit();
	}

	@Override
	public boolean abort() throws LoginException {
		return loginModule.get().abort();
	}

	@Override
	public boolean logout() throws LoginException {
		return loginModule.get().logout();
	}

}
