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
package org.argeo.security.ui.rap;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.IEntryPoint;
import org.eclipse.ui.PlatformUI;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * RAP entry point which logs out the currently authenticated user
 */
public class LogoutEntryPoint implements IEntryPoint {
	private final static Log log = LogFactory.getLog(LogoutEntryPoint.class);

	/**
	 * From org.springframework.security.context.
	 * HttpSessionContextIntegrationFilter
	 */
	protected static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	@Override
	public int createUI() {
		// create display
		PlatformUI.createDisplay();

		final ILoginContext loginContext = SecureRapActivator
				.createLoginContext(SecureRapActivator.CONTEXT_SPRING);
		try {
			loginContext.logout();
		} catch (LoginException e) {
			e.printStackTrace();
		}

		RWT.getRequest().getSession()
				.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
		SecurityContextHolder.clearContext();
		RWT.getRequest().getSession().setMaxInactiveInterval(1);

		if (log.isDebugEnabled())
			log.debug("Logged out session " + RWT.getSessionStore().getId());
		return 0;
	}
}
