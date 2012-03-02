package org.argeo.security.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.security.SystemAuthentication;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/** Provides base method for executing code with system authorization. */
public abstract class AbstractSystemExecution {
	static {
		// Forces Spring Security to use inheritable strategy
		// FIXME find a better place for forcing spring security mode
		// doesn't work for the time being
//		if (System.getProperty(SecurityContextHolder.SYSTEM_PROPERTY) == null)
//			SecurityContextHolder
//					.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	private final static Log log = LogFactory
			.getLog(AbstractSystemExecution.class);
	private AuthenticationManager authenticationManager;
	private String systemAuthenticationKey;

	/** Whether the current thread was authenticated by this component. */
	private ThreadLocal<Boolean> authenticatedBySelf = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return false;
		}
	};

	/**
	 * Authenticate the calling thread to the underlying
	 * {@link AuthenticationManager}
	 */
	protected void authenticateAsSystem() {
		if (authenticatedBySelf.get())
			return;
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication currentAuth = securityContext.getAuthentication();
		if (currentAuth != null) {
			if (!(currentAuth instanceof SystemAuthentication))
				throw new ArgeoException(
						"System execution on an already authenticated thread: "
								+ currentAuth + ", THREAD="
								+ Thread.currentThread().getId());
			return;
		}
		// Subject subject = Subject.getSubject(AccessController.getContext());
		// if (subject != null
		// && !subject.getPrincipals(Authentication.class).isEmpty())
		// throw new ArgeoException(
		// "There is already an authenticated subject: " + subject);

		String key = systemAuthenticationKey != null ? systemAuthenticationKey
				: System.getProperty(
						InternalAuthentication.SYSTEM_KEY_PROPERTY,
						InternalAuthentication.SYSTEM_KEY_DEFAULT);
		if (key == null)
			throw new ArgeoException("No system key defined");
		Authentication auth = authenticationManager
				.authenticate(new InternalAuthentication(key));
		securityContext.setAuthentication(auth);
		authenticatedBySelf.set(true);
		if (log.isTraceEnabled())
			log.trace("System authenticated");
	}

	// /** Removes the authentication from the calling thread. */
	// protected void deauthenticateAsSystem() {
	// // remove the authentication
	// // SecurityContext securityContext = SecurityContextHolder.getContext();
	// // securityContext.setAuthentication(null);
	// // authenticatedBySelf.set(false);
	// if (log.isTraceEnabled()) {
	// log.trace("System deauthenticated");
	// // Thread.dumpStack();
	// }
	// }

	/**
	 * Whether the current thread was authenticated by this component or a
	 * parent thread.
	 */
	protected Boolean isAuthenticatedBySelf() {
		return authenticatedBySelf.get();
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemAuthenticationKey(String systemAuthenticationKey) {
		this.systemAuthenticationKey = systemAuthenticationKey;
	}

}
