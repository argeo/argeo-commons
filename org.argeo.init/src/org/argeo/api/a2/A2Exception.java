package org.argeo.api.a2;

/** Unchecked A2 provisioning exception. */
public class A2Exception extends RuntimeException {
	private static final long serialVersionUID = 1927603558545397360L;

	public A2Exception(String message, Throwable e) {
		super(message, e);
	}

	public A2Exception(String message) {
		super(message);
	}

}
