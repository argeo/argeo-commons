package org.argeo.security.core;

import java.security.AccessController;

import javax.security.auth.Subject;

import org.argeo.ArgeoException;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/** Provides base method for executing code with system authorization. */
public abstract class AbstractSystemExecution {
	private AuthenticationManager authenticationManager;
	private String systemAuthenticationKey;

	/**
	 * Authenticate the calling thread to the underlying
	 * {@link AuthenticationManager}
	 */
	protected void authenticateAsSystem() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication currentAuth = securityContext.getAuthentication();
		if (currentAuth != null)
			throw new ArgeoException(
					"System execution on an already authenticated thread: "
							+ currentAuth + ", THREAD="
							+ Thread.currentThread().getId());

		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject != null
				&& !subject.getPrincipals(Authentication.class).isEmpty())
			throw new ArgeoException(
					"There is already an authenticated subject: " + subject);

		String key = systemAuthenticationKey != null ? systemAuthenticationKey
				: System.getProperty(
						InternalAuthentication.SYSTEM_KEY_PROPERTY,
						InternalAuthentication.SYSTEM_KEY_DEFAULT);
		if (key == null)
			throw new ArgeoException("No system key defined");
		Authentication auth = authenticationManager
				.authenticate(new InternalAuthentication(key));
		securityContext.setAuthentication(auth);
	}

	/** Removes the authentication from the calling thread. */
	protected void deauthenticateAsSystem() {
		// remove the authentication
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(null);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemAuthenticationKey(String systemAuthenticationKey) {
		this.systemAuthenticationKey = systemAuthenticationKey;
	}

}
