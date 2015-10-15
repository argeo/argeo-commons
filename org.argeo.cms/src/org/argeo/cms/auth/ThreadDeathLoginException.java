package org.argeo.cms.auth;

import javax.security.auth.login.LoginException;

public class ThreadDeathLoginException extends LoginException {
	private static final long serialVersionUID = 4359130889332276894L;

	private final ThreadDeath threadDeath;

	public ThreadDeathLoginException(String msg, ThreadDeath cause) {
		this.threadDeath = cause;
	}

	public ThreadDeath getThreadDeath() {
		return threadDeath;
	}
}