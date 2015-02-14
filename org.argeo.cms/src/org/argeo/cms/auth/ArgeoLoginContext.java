package org.argeo.cms.auth;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** Integrates JAAS with the Argeo platform */
public class ArgeoLoginContext extends LoginContext {
	private static ThreadLocal<ClassLoader> currentContextClassLoader = new ThreadLocal<ClassLoader>() {
		@Override
		protected ClassLoader initialValue() {
			return Thread.currentThread().getContextClassLoader();
		}

		@Override
		public void set(ClassLoader value) {
			throw new IllegalAccessError("Current class loader is read-only");
		}
	};

	public ArgeoLoginContext(String name, Subject subject,
			CallbackHandler callbackHandler) throws LoginException {
		super(setContextClassLoaderForName(name), subject, callbackHandler);
		// reset current context classloader
		Thread.currentThread().setContextClassLoader(
				currentContextClassLoader.get());
		currentContextClassLoader.remove();
	}

	/**
	 * Set the context classloader
	 * 
	 * @return the passed name, in order to chain calls in the constructor
	 */
	private static String setContextClassLoaderForName(String name) {
		// store current context class loader;
		currentContextClassLoader.get();
		Thread.currentThread().setContextClassLoader(
				ArgeoLoginContext.class.getClassLoader());
		return name;
	}

	@Override
	public void login() throws LoginException {
		super.login();
	}

	@Override
	public void logout() throws LoginException {
		super.logout();
	}
}
