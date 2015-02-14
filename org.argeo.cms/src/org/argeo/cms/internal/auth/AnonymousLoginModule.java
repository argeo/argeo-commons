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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.argeo.cms.internal.kernel.Activator;
import org.argeo.util.LocaleCallback;
import org.argeo.util.LocaleUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Login module which caches one subject per thread. */
public class AnonymousLoginModule extends AbstractLoginModule {
	private String anonymousRole = "ROLE_ANONYMOUS";
	/** Comma separated list of locales */
	private String availableLocales = null;

	@Override
	protected Authentication processLogin(CallbackHandler callbackHandler)
			throws LoginException, UnsupportedCallbackException, IOException,
			InterruptedException {
		Locale selectedLocale = null;
		// multi locale
		if (availableLocales != null && !availableLocales.trim().equals("")) {
			LocaleCallback localeCallback = new LocaleCallback(availableLocales);
			callbackHandler.handle(new Callback[] { localeCallback });
			selectedLocale = localeCallback.getSelectedLocale();
		} else {
			callbackHandler.handle(new Callback[] {});
		}

		List<SimpleGrantedAuthority> authorities = Collections
				.singletonList(new SimpleGrantedAuthority(anonymousRole));
		AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
				Activator.getSystemKey(), null, authorities);

		Authentication auth = getAuthenticationManager().authenticate(
				anonymousToken);

		if (selectedLocale != null)
			LocaleUtils.threadLocale.set(selectedLocale);
		return auth;
	}
}
