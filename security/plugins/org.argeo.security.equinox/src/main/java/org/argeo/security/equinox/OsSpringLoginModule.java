/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.equinox;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

/** Login module which caches one subject per thread. */
public class OsSpringLoginModule extends SecurityContextLoginModule {
	// private final static Log log =
	// LogFactory.getLog(OsSpringLoginModule.class);

	private AuthenticationManager authenticationManager;

	private Subject subject;

	public OsSpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		this.subject = subject;
	}

	public boolean login() throws LoginException {
		// thread already logged in
		if (SecurityContextHolder.getContext().getAuthentication() != null)
			return super.login();

		OsAuthenticationToken oat = new OsAuthenticationToken();
		Authentication authentication = authenticationManager.authenticate(oat);
		registerAuthentication(authentication);
		return super.login();
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().clear();
		return super.logout();
	}

	/**
	 * Register an {@link Authentication} in the security context.
	 * 
	 * @param authentication
	 *            has to implement {@link Authentication}.
	 */
	protected void registerAuthentication(Object authentication) {
		SecurityContextHolder.getContext().setAuthentication(
				(Authentication) authentication);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}
}
