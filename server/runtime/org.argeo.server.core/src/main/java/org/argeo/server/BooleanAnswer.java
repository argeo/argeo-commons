package org.argeo.server;


/** Answer to an execution of a remote service which performed changes. */
public class BooleanAnswer {
	private Boolean value = Boolean.TRUE;

	/** Canonical constructor */
	public BooleanAnswer(Boolean status) {
		this.value = status;
	}

	/** Empty constructor */
	public BooleanAnswer() {
	}

	public Boolean getValue() {
		return value;
	}

}
