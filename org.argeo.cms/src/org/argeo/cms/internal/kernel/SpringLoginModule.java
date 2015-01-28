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
package org.argeo.cms.internal.kernel;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.NodeAuthenticationToken;
import org.argeo.util.LocaleCallback;
import org.argeo.util.LocaleUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.jaas.SecurityContextLoginModule;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Login module which caches one subject per thread. */
public class SpringLoginModule extends SecurityContextLoginModule {
	final static String NODE_REPO_URI = "argeo.node.repo.uri";

	private final static Log log = LogFactory.getLog(SpringLoginModule.class);

	private AuthenticationManager authenticationManager;

	private CallbackHandler callbackHandler;

	private Subject subject;

	private Long waitBetweenFailedLoginAttempts = 5 * 1000l;

	private Boolean remote = false;
	private Boolean anonymous = false;
	/** Comma separated list of locales */
	private String availableLocales = "";

	private String key = null;
	private String anonymousRole = "ROLE_ANONYMOUS";

	public SpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		this.callbackHandler = callbackHandler;
		this.subject = subject;
	}

	public boolean login() throws LoginException {
		try {
			// thread already logged in
			if (SecurityContextHolder.getContext().getAuthentication() != null)
				return super.login();

			if (remote && anonymous)
				throw new LoginException(
						"Cannot have a Spring login module which is remote and anonymous");

			// reset all principals and credentials
			if (log.isTraceEnabled())
				log.trace("Resetting all principals and credentials of "
						+ subject);
			subject.getPrincipals().clear();
			subject.getPrivateCredentials().clear();
			subject.getPublicCredentials().clear();

			Locale selectedLocale = null;
			// deals first with public access since it's simple
			if (anonymous) {
				// multi locale
				if (callbackHandler != null && availableLocales != null
						&& !availableLocales.trim().equals("")) {
					LocaleCallback localeCallback = new LocaleCallback(
							availableLocales);
					callbackHandler.handle(new Callback[] { localeCallback });
					selectedLocale = localeCallback.getSelectedLocale();
				}

				// TODO integrate with JCR?
				Object principal = UUID.randomUUID().toString();
				List<SimpleGrantedAuthority> authorities = Collections
						.singletonList(new SimpleGrantedAuthority(anonymousRole));
				AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
						key, principal, authorities);
				Authentication auth = authenticationManager
						.authenticate(anonymousToken);
				registerAuthentication(auth);
			} else {
				if (callbackHandler == null)
					throw new LoginException("No call back handler available");

				// ask for username and password
				NameCallback nameCallback = new NameCallback("User");
				PasswordCallback passwordCallback = new PasswordCallback(
						"Password", false);
				final String defaultNodeUrl = System
						.getProperty(NODE_REPO_URI,
								"http://localhost:7070/org.argeo.jcr.webapp/remoting/node");
				NameCallback urlCallback = new NameCallback("Site URL",
						defaultNodeUrl);
				LocaleCallback localeCallback = new LocaleCallback(
						availableLocales);

				// handle callbacks
				if (remote)
					callbackHandler.handle(new Callback[] { nameCallback,
							passwordCallback, urlCallback, localeCallback });
				else
					callbackHandler.handle(new Callback[] { nameCallback,
							passwordCallback, localeCallback });

				selectedLocale = localeCallback.getSelectedLocale();

				// create credentials
				final String username = nameCallback.getName();
				if (username == null || username.trim().equals(""))
					return false;

				char[] password = {};
				if (passwordCallback.getPassword() != null)
					password = passwordCallback.getPassword();

				NodeAuthenticationToken credentials;
				if (remote) {
					String url = urlCallback.getName();
					credentials = new NodeAuthenticationToken(username,
							password, url);
				} else {
					credentials = new NodeAuthenticationToken(username,
							password);
				}

				Authentication authentication;
				try {
					authentication = authenticationManager
							.authenticate(credentials);
				} catch (BadCredentialsException e) {
					// wait between failed login attempts
					Thread.sleep(waitBetweenFailedLoginAttempts);
					throw e;
				}
				registerAuthentication(authentication);
				subject.getPrincipals().add(authentication);
			}

			if (selectedLocale != null)
				LocaleUtils.threadLocale.set(selectedLocale);

			return super.login();
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

	/** Authenticates on a remote node */
	public void setRemote(Boolean remote) {
		this.remote = remote;
	}

	/**
	 * Request anonymous authentication (incompatible with remote)
	 */
	public void setAnonymous(Boolean anonymous) {
		this.anonymous = anonymous;
	}

	/** Role identifying an anonymous user */
	public void setAnonymousRole(String anonymousRole) {
		this.anonymousRole = anonymousRole;
	}

	/** System key */
	public void setKey(String key) {
		this.key = key;
	}

	public void setAvailableLocales(String locales) {
		this.availableLocales = locales;
	}

}
