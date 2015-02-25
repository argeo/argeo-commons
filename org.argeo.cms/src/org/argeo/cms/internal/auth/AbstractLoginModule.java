/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.internal.auth;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.ArgeoException;
import org.argeo.cms.internal.kernel.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Login module which caches one subject per thread. */
public abstract class AbstractLoginModule implements LoginModule {
	// private final static Log log = LogFactory
	// .getLog(AbstractSpringLoginModule.class);
	private CallbackHandler callbackHandler;
	private Subject subject;

	private Authentication authentication;

	// state
	private BundleContext bundleContext;
	private ServiceReference<AuthenticationManager> authenticationManager;

	protected abstract Authentication processLogin(
			CallbackHandler callbackHandler) throws LoginException,
			UnsupportedCallbackException, IOException, InterruptedException;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		this.callbackHandler = callbackHandler;
		this.subject = subject;
		this.bundleContext = Activator.getBundleContext();
		this.authenticationManager = bundleContext
				.getServiceReference(AuthenticationManager.class);
	}

	@Override
	public boolean login() throws LoginException {
		try {
			// thread already logged in
			Authentication currentAuth = SecurityContextHolder.getContext()
					.getAuthentication();
			if (currentAuth != null) {
				if (subject.getPrincipals(Authentication.class).size() == 0) {
					// throw new LoginException(
					// "Security context set but not Authentication principal");
				} else {
					Authentication principal = subject
							.getPrincipals(Authentication.class).iterator()
							.next();
					if (principal != currentAuth)
						throw new LoginException(
								"Already authenticated with a different auth");
				}
				return true;
			}

			// if (callbackHandler == null)
			// throw new LoginException("No callback handler available");

			authentication = processLogin(callbackHandler);
			if (authentication != null) {
				SecurityContextHolder.getContext().setAuthentication(
						authentication);
				return true;
			} else {
				throw new LoginException("No authentication returned");
			}
		} catch (LoginException e) {
			throw e;
		} catch (ThreadDeath e) {
			LoginException le = new LoginException(
					"Spring Security login thread died");
			le.initCause(e);
			throw le;
		} catch (Exception e) {
			LoginException le = new LoginException(
					"Spring Security login failed");
			le.initCause(e);
			throw le;
		}
	}

	@Override
	public boolean logout() throws LoginException {
		SecurityContextHolder.getContext().setAuthentication(null);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		SecurityContextHolder.getContext().setAuthentication(null);
		return true;
	}

	/**
	 * Return the related {@link BundleContext} (never null), or throws an
	 * Exception if the login module was not properly initialised.
	 */
	protected BundleContext getBundleContext() {
		if (bundleContext == null)
			throw new ArgeoException("No bundle context provided");
		return bundleContext;
	}

	AuthenticationManager getAuthenticationManager() {
		BundleContext bc = getBundleContext();
		assert authenticationManager != null;
		return bc.getService(authenticationManager);
	}

	// protected UserAdmin getUserAdmin(BundleContextCallback
	// bundleContextCallback) {
	// BundleContext bc = bundleContextCallback.getBundleContext();
	// return bc.getService(bc.getServiceReference(UserAdmin.class));
	// }

	protected Subject getSubject() {
		return subject;
	}
}
