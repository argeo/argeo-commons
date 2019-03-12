package org.argeo.sync;

/** Commons exception for sync */
public class SyncException extends RuntimeException {
	private static final long serialVersionUID = -3371314343580218538L;

	public SyncException(String message) {
		super(message);
	}

	public SyncException(String message, Throwable cause) {
		super(message, cause);
	}

	public SyncException(Object source, Object target, Throwable cause) {
		super("Cannot sync from " + source + " to " + target, cause);
	}
}
