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
package org.argeo.security.core;

import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.jcr.ArgeoNames;
import org.argeo.util.LocaleCallback;
import org.argeo.util.LocaleUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder;

/** Login module which caches one subject per thread. */
public class UserAdminLoginModule implements LoginModule {
	// private final static Log log = LogFactory
	// .getLog(UserAdminLoginModule.class);

	private CallbackHandler callbackHandler;

	private Subject subject;

	private Long waitBetweenFailedLoginAttempts = 5 * 1000l;

	/** Comma separated list of locales */
	private String availableLocales = "";

	private AuthorizationPrincipal auth = null;
	private Locale selectedLocale = null;

	private LdapShaPasswordEncoder shaPasswordEncoder = new LdapShaPasswordEncoder();

	public UserAdminLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		this.callbackHandler = callbackHandler;
		this.subject = subject;
	}

	public boolean login() throws LoginException {
		try {
			// TODO thread already logged in
			// AuthorizationPrincipal principal = subject
			// .getPrincipals(AuthorizationPrincipal.class).iterator();

			if (callbackHandler == null)
				throw new LoginException("No call back handler available");

			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);
			LocaleCallback localeCallback = new LocaleCallback(availableLocales);
			BundleContextCallback bundleContextCallback = new BundleContextCallback();

			callbackHandler.handle(new Callback[] { nameCallback,
					passwordCallback, localeCallback, bundleContextCallback });

			selectedLocale = localeCallback.getSelectedLocale();

			// create credentials
			final String username = nameCallback.getName();
			if (username == null || username.trim().equals(""))
				return false;

			char[] password = {};
			if (passwordCallback.getPassword() != null)
				password = passwordCallback.getPassword();

			BundleContext bc = bundleContextCallback.getBundleContext();
			UserAdmin userAdmin = bc.getService(bc
					.getServiceReference(UserAdmin.class));

			User user = (User) userAdmin.getRole(username);
			// TODO use hash
			boolean authenticated = user.hasCredential(
					ArgeoNames.ARGEO_PASSWORD, new String(password));

			if (!authenticated) {
				// wait between failed login attempts
				Thread.sleep(waitBetweenFailedLoginAttempts);
				return false;
			}

			Authorization authorization = userAdmin.getAuthorization(user);
			auth = new AuthorizationPrincipal(authorization);
			return true;
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
		subject.getPrincipals(AuthorizationPrincipal.class).remove(auth);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		subject.getPrincipals().add(auth);
		if (selectedLocale != null)
			LocaleUtils.threadLocale.set(selectedLocale);
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		auth = null;
		selectedLocale = null;
		return true;
	}

	public void setAvailableLocales(String locales) {
		this.availableLocales = locales;
	}
}