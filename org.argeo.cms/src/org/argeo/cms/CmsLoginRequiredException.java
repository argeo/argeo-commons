package org.argeo.cms;

/** Throwing this exception triggers redirection to a login page. */
public class CmsLoginRequiredException extends CmsException {
	private static final long serialVersionUID = 7009402894657958151L;

	public CmsLoginRequiredException() {
		super("Login is required");
	}

	public CmsLoginRequiredException(String message, Throwable e) {
		super(message, e);
	}

	public CmsLoginRequiredException(String message) {
		super(message);
	}

}
