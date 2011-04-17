package org.argeo.security.core;

import java.security.AccessController;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
		try {
			wrapWithSystemAuthentication(Executors.callable(runnable)).call();
		} catch (Exception e) {
			throw new ArgeoException(
					"Exception when running system authenticated task", e);
		}
	}

	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(
				wrapWithSystemAuthentication(task));
		future.run();
		return future;
	}

	protected <T> Callable<T> wrapWithSystemAuthentication(
			final Callable<T> runnable) {
		return new Callable<T>() {

			public T call() throws Exception {
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
					return runnable.call();
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
