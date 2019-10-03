package org.argeo.eclipse.ui.specific;

/** Exception related to SWT/RWT single sourcing. */
public class SingleSourcingException extends RuntimeException {
	private static final long serialVersionUID = -727700418055348468L;

	public SingleSourcingException(String message, Throwable cause) {
		super(message, cause);
	}

	public SingleSourcingException(String message) {
		super(message);
	}

}
