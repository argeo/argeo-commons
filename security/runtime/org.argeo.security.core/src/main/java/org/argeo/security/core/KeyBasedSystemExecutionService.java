package org.argeo.security.core;

import org.argeo.security.SystemExecutionService;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

public class KeyBasedSystemExecutionService implements SystemExecutionService,
		TaskExecutor {
	private AuthenticationManager authenticationManager;
	private String systemAuthenticationKey;

	public void execute(Runnable runnable) {
		wrapWithSystemAuthentication(runnable).run();
	}

	public TaskExecutor createSystemAuthenticatedTaskExecutor() {
		return new SimpleAsyncTaskExecutor() {
			private static final long serialVersionUID = -8126773862193265020L;

			@Override
			public Thread createThread(Runnable runnable) {
				return super
						.createThread(wrapWithSystemAuthentication(runnable));
			}

		};
	}

	protected Runnable wrapWithSystemAuthentication(final Runnable runnable) {
		return new Runnable() {

			public void run() {
				SecurityContext securityContext = SecurityContextHolder
						.getContext();
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
