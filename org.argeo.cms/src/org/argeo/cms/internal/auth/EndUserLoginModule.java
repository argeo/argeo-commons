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
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginException;

import org.argeo.security.NodeAuthenticationToken;
import org.argeo.util.LocaleCallback;
import org.argeo.util.LocaleUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/** Authenticates an end user */
public class EndUserLoginModule extends AbstractLoginModule {
	final static String NODE_REPO_URI = "argeo.node.repo.uri";

	private Long waitBetweenFailedLoginAttempts = 5 * 1000l;

	private Boolean remote = false;
	/** Comma separated list of locales */
	private String availableLocales = "";

	@Override
	protected Authentication processLogin(CallbackHandler callbackHandler)
			throws LoginException, UnsupportedCallbackException, IOException,
			InterruptedException {
		if (callbackHandler == null)
			return null;

		// ask for username and password
		NameCallback nameCallback = new NameCallback("User");
		PasswordCallback passwordCallback = new PasswordCallback("Password",
				false);
		final String defaultNodeUrl = System.getProperty(NODE_REPO_URI,
				"http://localhost:7070/org.argeo.jcr.webapp/remoting/node");
		NameCallback urlCallback = new NameCallback("Site URL", defaultNodeUrl);
		LocaleCallback localeCallback = new LocaleCallback(availableLocales);
		// handle callbacks
		if (remote)
			callbackHandler.handle(new Callback[] { nameCallback,
					passwordCallback, urlCallback, localeCallback });
		else
			callbackHandler.handle(new Callback[] { nameCallback,
					passwordCallback, localeCallback });

		Locale selectedLocale = localeCallback.getSelectedLocale();

		// create credentials
		final String username = nameCallback.getName();
		if (username == null || username.trim().equals(""))
			throw new CredentialNotFoundException("No credentials provided");

		char[] password = {};
		if (passwordCallback.getPassword() != null)
			password = passwordCallback.getPassword();
		else
			throw new CredentialNotFoundException("No credentials provided");

		NodeAuthenticationToken credentials;
		if (remote) {
			String url = urlCallback.getName();
			credentials = new NodeAuthenticationToken(username, password, url);
		} else {
			credentials = new NodeAuthenticationToken(username, password);
		}

		Authentication auth;
		try {
			auth = getAuthenticationManager().authenticate(credentials);
		} catch (BadCredentialsException e) {
			// wait between failed login attempts
			Thread.sleep(waitBetweenFailedLoginAttempts);
			throw e;
		}

		if (selectedLocale != null)
			LocaleUtils.threadLocale.set(selectedLocale);

		return auth;
	}

	@Override
	public boolean commit() throws LoginException {
		return super.commit();
	}
}
