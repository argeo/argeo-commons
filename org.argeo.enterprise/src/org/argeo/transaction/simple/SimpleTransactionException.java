package org.argeo.transaction.simple;

public class SimpleTransactionException extends RuntimeException {
	private static final long serialVersionUID = -7465792070797750212L;

	public SimpleTransactionException(String message) {
		super(message);
	}

	public SimpleTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

}
