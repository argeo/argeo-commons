package org.argeo.eclipse.ui.fs;

/** Files specific exception */
public class FsUiException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public FsUiException(String message) {
		super(message);
	}

	public FsUiException(String message, Throwable e) {
		super(message, e);
	}
}
