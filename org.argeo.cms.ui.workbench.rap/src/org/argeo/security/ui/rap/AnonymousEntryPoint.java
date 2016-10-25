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

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.NodeConstants;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * RAP entry point which authenticates the subject as anonymous, for public
 * unauthenticated access.
 */
public class AnonymousEntryPoint implements EntryPoint {
	private final static Log log = LogFactory.getLog(AnonymousEntryPoint.class);

	/**
	 * How many seconds to wait before invalidating the session if the user has
	 * not yet logged in.
	 */
	private Integer sessionTimeout = 5 * 60;

	@Override
	public int createUI() {
		RWT.getRequest().getSession().setMaxInactiveInterval(sessionTimeout);

		// if (log.isDebugEnabled())
		// log.debug("Anonymous THREAD=" + Thread.currentThread().getId()
		// + ", sessionStore=" + RWT.getSessionStore().getId());

		final Display display = PlatformUI.createDisplay();
		Subject subject = new Subject();

		final LoginContext loginContext;
		try {
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					subject);
			loginContext.login();
		} catch (LoginException e1) {
			throw new CmsException("Cannot initialize login context", e1);
		}

		// identify after successful login
		if (log.isDebugEnabled())
			log.debug("Authenticated " + subject);
		final String username = subject.getPrincipals().iterator().next()
				.getName();

		// Logout callback when the display is disposed
		display.disposeExec(new Runnable() {
			public void run() {
				log.debug("Display disposed");
				logout(loginContext, username);
			}
		});

		//
		// RUN THE WORKBENCH
		//
		Integer returnCode = null;
		try {
			returnCode = Subject.doAs(subject, new PrivilegedAction<Integer>() {
				public Integer run() {
					RapWorkbenchAdvisor workbenchAdvisor = new RapWorkbenchAdvisor(
							null);
					int result = PlatformUI.createAndRunWorkbench(display,
							workbenchAdvisor);
					return new Integer(result);
				}
			});
			logout(loginContext, username);
			if (log.isTraceEnabled())
				log.trace("Return code " + returnCode);
		} finally {
			display.dispose();
		}
		return 1;
	}

	private void logout(LoginContext loginContext, String username) {
		try {
			loginContext.logout();
			log.info("Logged out " + (username != null ? username : "")
					+ " (THREAD=" + Thread.currentThread().getId() + ")");
		} catch (LoginException e) {
			log.error("Erorr when logging out", e);
		}
	}
}
