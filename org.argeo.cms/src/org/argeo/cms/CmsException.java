package org.argeo.cms;

/** CMS specific exceptions. */
public class CmsException extends RuntimeException {
	private static final long serialVersionUID = -5341764743356771313L;

	public CmsException(String message) {
		super(message);
	}

	public CmsException(String message, Throwable e) {
		super(message, e);
	}

}
