package org.argeo.jcr.fs;


/** Exception related to the JCR FS */
public class JcrFsException extends RuntimeException {
	private static final long serialVersionUID = -7973896038244922980L;

	public JcrFsException(String message, Throwable e) {
		super(message, e);
	}

	public JcrFsException(String message) {
		super(message);
	}
}
