package org.argeo.cms.auth;

import org.argeo.cms.CmsException;

/** Throwing this exception triggers redirection to a login page. */
public class LoginRequiredException extends CmsException {
	private static final long serialVersionUID = 7009402894657958151L;

	public LoginRequiredException() {
		super("Login is required");
	}

	public LoginRequiredException(String message, Throwable e) {
		super(message, e);
	}

	public LoginRequiredException(String message) {
		super(message);
	}

}
