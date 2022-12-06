package org.argeo.eclipse.ui;

/** CMS specific exceptions. */
public class EclipseUiException extends RuntimeException {
	private static final long serialVersionUID = -5341764743356771313L;

	public EclipseUiException(String message) {
		super(message);
	}

	public EclipseUiException(String message, Throwable e) {
		super(message, e);
	}

}
