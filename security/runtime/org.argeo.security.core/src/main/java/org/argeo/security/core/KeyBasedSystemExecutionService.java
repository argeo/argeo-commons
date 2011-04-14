package org.argeo.security.core;

import java.security.AccessController;

import javax.security.auth.Subject;

import org.argeo.ArgeoException;
import org.argeo.security.SystemExecutionService;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * Implementation of a {@link SystemExecutionService} using a key-based
 * {@link InternalAuthentication}
 */
public class KeyBasedSystemExecutionService implements SystemExecutionService {
	private AuthenticationManager authenticationManager;
	private String systemAuthenticationKey;

	public void execute(Runnable runnable) {
		wrapWithSystemAuthentication(runnable).run();
	}

	protected Runnable wrapWithSystemAuthentication(final Runnable runnable) {
		return new Runnable() {

			public void run() {
				SecurityContext securityContext = SecurityContextHolder
						.getContext();
				Authentication currentAuth = securityContext
						.getAuthentication();
				if (currentAuth != null)
					throw new ArgeoException(
							"System execution on an already authenticated thread: "
									+ currentAuth + ", THREAD="
									+ Thread.currentThread().getId());

				Subject subject = Subject.getSubject(AccessController
						.getContext());
				if (subject != null
						&& !subject.getPrincipals(Authentication.class)
								.isEmpty())
					throw new ArgeoException(
							"There is already an authenticated subject: "
									+ subject);

				Authentication auth = authenticationManager
						.authenticate(new InternalAuthentication(
								systemAuthenticationKey));
				securityContext.setAuthentication(auth);
				try {
					runnable.run();
				} finally {
					// remove the authentication
					securityContext.setAuthentication(null);
				}
			}
		};
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemAuthenticationKey(String systemAuthenticationKey) {
		this.systemAuthenticationKey = systemAuthenticationKey;
	}

}
