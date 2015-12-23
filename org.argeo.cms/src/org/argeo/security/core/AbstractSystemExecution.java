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

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/** Provides base method for executing code with system authorization. */
public abstract class AbstractSystemExecution {
	private final static Log log = LogFactory
			.getLog(AbstractSystemExecution.class);
	private final Subject subject = new Subject();

	private final String loginModule = "SYSTEM";

	/**
	 * Authenticate the calling thread to the underlying
	 * {@link AuthenticationManager}
	 */
	protected void authenticateAsSystem() {
		try {
			LoginContext lc = new LoginContext(loginModule, subject);
			lc.login();
		} catch (LoginException e) {
			throw new ArgeoException("Cannot login as system", e);
		}
		if (log.isTraceEnabled())
			log.trace("System authenticated");
	}

	protected void deauthenticateAsSystem() {
		try {
			LoginContext lc = new LoginContext(loginModule, subject);
			lc.logout();
		} catch (LoginException e) {
			throw new ArgeoException("Cannot logout as system", e);
		}
	}

	protected Subject getSubject() {
		return subject;
	}
}