package org.argeo.security.core;

import org.argeo.security.ArgeoSecurityService;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class SystemAuthenticatedTaskExecutor extends SimpleAsyncTaskExecutor {
	private static final long serialVersionUID = 453384889461147359L;

	private ArgeoSecurityService securityService;

	@Override
	public Thread createThread(Runnable runnable) {
		return super.createThread(securityService
				.wrapWithSystemAuthentication(runnable));
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
