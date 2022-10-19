package org.argeo.cms.swt;

/** @deprecated Use standard Java {@link RuntimeException} instead. */
@Deprecated
public class CmsException extends RuntimeException {
	private static final long serialVersionUID = -5341764743356771313L;

	public CmsException(String message) {
		super(message);
	}

	public CmsException(String message, Throwable e) {
		super(message, e);
	}

}
