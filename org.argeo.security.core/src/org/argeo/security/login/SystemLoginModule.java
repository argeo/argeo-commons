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
package org.argeo.security.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.argeo.security.SecurityUtils;
import org.argeo.security.core.InternalAuthentication;
import org.springframework.security.core.Authentication;

/** Login module which caches one subject per thread. */
public class SystemLoginModule extends AbstractSpringSecurityLoginModule {
	@Override
	protected Authentication processLogin(CallbackHandler callbackHandler)
			throws LoginException, UnsupportedCallbackException, IOException,
			InterruptedException {
		BundleContextCallback bundleContextCallback = new BundleContextCallback();
		callbackHandler.handle(new Callback[] { bundleContextCallback });
		InternalAuthentication anonymousToken = new InternalAuthentication(
				SecurityUtils.getStaticKey());
		return getAuthenticationManager(bundleContextCallback).authenticate(
				anonymousToken);
	}
}
