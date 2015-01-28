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
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.ErrorFeedback;
import org.argeo.security.ui.dialogs.DefaultLoginDialog;
import org.argeo.util.LocaleUtils;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * RAP entry point with login capabilities. Once the user has been
 * authenticated, the workbench is run as a privileged action by the related
 * subject.
 */
public class SecureEntryPoint implements EntryPoint {
	private final static Log log = LogFactory.getLog(SecureEntryPoint.class);

	/**
	 * From org.springframework.security.context.
	 * HttpSessionContextIntegrationFilter
	 */
	protected static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	/**
	 * How many seconds to wait before invalidating the session if the user has
	 * not yet logged in.
	 */
	private Integer loginTimeout = 1 * 60;
	// TODO make it configurable
	/** Default session timeout is 8 hours (European working day length) */
	private Integer sessionTimeout = 8 * 60 * 60;

	/** Override to provide an application specific workbench advisor */
	protected RapWorkbenchAdvisor createRapWorkbenchAdvisor(String username) {
		return new RapWorkbenchAdvisor(username);
	}

	@Override
	public final int createUI() {
		// Short login timeout so that the modal dialog login doesn't hang
		// around too long
		RWT.getRequest().getSession().setMaxInactiveInterval(loginTimeout);

		// Try to load security context thanks to the session processing filter
		HttpServletRequest httpRequest = RWT.getRequest();
		HttpSession httpSession = httpRequest.getSession();
		Object contextFromSessionObject = httpSession
				.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		if (contextFromSessionObject != null)
			SecurityContextHolder
					.setContext((SecurityContext) contextFromSessionObject);

		// if (log.isDebugEnabled())
		// log.debug("THREAD=" + Thread.currentThread().getId()
		// + ", sessionStore=" + RWT.getSessionStore().getId()
		// + ", remote user=" + httpRequest.getRemoteUser());

		// create display
		final Display display = PlatformUI.createDisplay();
		Subject subject = new Subject();

		// log in
		BundleContext bc = SecureRapActivator.getActivator().getBundleContext();
		final LoginModule loginModule = bc.getService(bc
				.getServiceReference(LoginModule.class));
		loginModule.initialize(subject,
				new DefaultLoginDialog(display.getActiveShell()), null, null);
		tryLogin: while (subject.getPrincipals(Authentication.class).size() == 0) {
			try {
				if (!loginModule.login()) {
					throw new ArgeoException("Login failed");
				}

				if (subject.getPrincipals(Authentication.class).size() == 0)
					throw new ArgeoException("Login succeeded but no auth");// fatal
				
				// add security context to session
				if (httpSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY) == null)
					httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
							SecurityContextHolder.getContext());
				// add thread locale to RWT session
				log.info("Locale "+LocaleUtils.threadLocale.get());
				RWT.setLocale(LocaleUtils.threadLocale.get());

				// Once the user is logged in, longer session timeout
				RWT.getRequest().getSession()
						.setMaxInactiveInterval(sessionTimeout);

				if (log.isDebugEnabled())
					log.debug("Authenticated " + subject);
			} catch (LoginException e) {
				BadCredentialsException bce = wasCausedByBadCredentials(e);
				if (bce != null) {
					MessageDialog.openInformation(display.getActiveShell(),
							"Bad Credentials", bce.getMessage());
					// retry login
					continue tryLogin;
				}
				return processLoginDeath(display, e);
			}
		}

		final String username = subject.getPrincipals(Authentication.class)
				.iterator().next().getName();
		// Logout callback when the display is disposed
		display.disposeExec(new Runnable() {
			public void run() {
				log.debug("Display disposed");
				// logout(loginContext, username);
				try {
					loginModule.logout();
				} catch (LoginException e) {
					log.error("Error when logging out", e);
				}
			}
		});

		//
		// RUN THE WORKBENCH
		//
		Integer returnCode = null;
		try {
			returnCode = Subject.doAs(subject, new PrivilegedAction<Integer>() {
				public Integer run() {
					RapWorkbenchAdvisor workbenchAdvisor = createRapWorkbenchAdvisor(username);
					int result = PlatformUI.createAndRunWorkbench(display,
							workbenchAdvisor);
					return new Integer(result);
				}
			});
			// logout(loginContext, username);
		} finally {
			display.dispose();
		}
		return returnCode;
	}

	private Integer processLoginDeath(Display display, LoginException e) {
		// check thread death
		ThreadDeath td = wasCausedByThreadDeath(e);
		if (td != null) {
			display.dispose();
			throw td;
		}
		if (!display.isDisposed()) {
			ErrorFeedback.show("Unexpected exception during authentication", e);
			// this was not just bad credentials or death thread
			RWT.getRequest().getSession().setMaxInactiveInterval(1);
			display.dispose();
			return -1;
		} else {
			throw new ArgeoException(
					"Unexpected exception during authentication", e);
		}

	}

	/** Recursively look for {@link BadCredentialsException} in the root causes. */
	private BadCredentialsException wasCausedByBadCredentials(Throwable t) {
		if (t instanceof BadCredentialsException)
			return (BadCredentialsException) t;

		if (t.getCause() != null)
			return wasCausedByBadCredentials(t.getCause());
		else
			return null;
	}

	/**
	 * If there is a {@link ThreadDeath} in the root causes, rethrow it
	 * (important for RAP cleaning mechanism)
	 */
	protected ThreadDeath wasCausedByThreadDeath(Throwable t) {
		if (t instanceof ThreadDeath)
			return (ThreadDeath) t;

		if (t.getCause() != null)
			return wasCausedByThreadDeath(t.getCause());
		else
			return null;
	}

	protected void logout(ILoginContext secureContext, String username) {
		try {
			HttpServletRequest httpRequest = RWT.getRequest();
			HttpSession httpSession = httpRequest.getSession();
			httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY, null);
			RWT.getRequest().getSession().setMaxInactiveInterval(1);
			SecurityContextHolder.clearContext();
			secureContext.logout();
			log.info("Logged out " + (username != null ? username : "")
					+ " (THREAD=" + Thread.currentThread().getId() + ")");
		} catch (LoginException e) {
			log.error("Erorr when logging out", e);
		}
	}
}
