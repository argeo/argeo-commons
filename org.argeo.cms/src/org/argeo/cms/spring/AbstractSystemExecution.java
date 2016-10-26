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
package org.argeo.cms.spring;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.NodeConstants;

/** Provides base method for executing code with system authorization. */
abstract class AbstractSystemExecution {
	private final static Log log = LogFactory.getLog(AbstractSystemExecution.class);
	private final Subject subject = new Subject();

	/**
	 * Authenticate the calling thread to the underlying
	 * {@link AuthenticationManager}
	 */
	protected void authenticateAsSystem() {
		ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_DATA_ADMIN, subject);
			lc.login();
		} catch (LoginException e) {
			throw new CmsException("Cannot login as system", e);
		} finally {
			Thread.currentThread().setContextClassLoader(origClassLoader);
		}
		if (log.isTraceEnabled())
			log.trace("System authenticated");
	}

	protected void deauthenticateAsSystem() {
		ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_DATA_ADMIN, subject);
			lc.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot logout as system", e);
		} finally {
			Thread.currentThread().setContextClassLoader(origClassLoader);
		}
	}

	protected Subject getSubject() {
		return subject;
	}
}
