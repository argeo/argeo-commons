package org.argeo.maintenance;

public class MaintenanceException extends RuntimeException {
	private static final long serialVersionUID = -4571088120514827735L;

	public MaintenanceException(String message) {
		super(message);
	}

	public MaintenanceException(String message, Throwable cause) {
		super(message, cause);
	}
}
