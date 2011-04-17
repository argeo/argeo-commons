package org.argeo.security.jcr;

import java.util.concurrent.Callable;

import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.spring.ThreadBoundSession;
import org.argeo.security.SystemExecutionService;

/** Thread bounded JCR session which logins as system authentication. */
public class SystemSession extends ThreadBoundSession {
	private SystemExecutionService systemExecutionService;

	@Override
	protected Session login() {
		try {
			return systemExecutionService.submit(new Callable<Session>() {
				public Session call() throws Exception {
					return SystemSession.super.login();
				}
			}).get();
		} catch (Exception e) {
			throw new ArgeoException(
					"Cannot login to JCR with system authentication", e);
		}
	}

	public void setSystemExecutionService(
			SystemExecutionService systemExecutionService) {
		this.systemExecutionService = systemExecutionService;
	}

}
