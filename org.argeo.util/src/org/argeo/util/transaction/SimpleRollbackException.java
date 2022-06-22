package org.argeo.util.transaction;

/** Internal unchecked rollback exception. */
class SimpleRollbackException extends RuntimeException {
	private static final long serialVersionUID = 8055601819719780566L;

	public SimpleRollbackException() {
		super();
	}

	public SimpleRollbackException(Throwable cause) {
		super(cause);
	}

}
