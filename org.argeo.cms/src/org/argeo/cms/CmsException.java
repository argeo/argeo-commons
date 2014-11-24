package org.argeo.cms;

import org.argeo.ArgeoException;

/** CMS specific exceptions. */
public class CmsException extends ArgeoException {
	private static final long serialVersionUID = -5341764743356771313L;

	public CmsException(String message) {
		super(message);
	}

	public CmsException(String message, Throwable e) {
		super(message, e);
	}

}
