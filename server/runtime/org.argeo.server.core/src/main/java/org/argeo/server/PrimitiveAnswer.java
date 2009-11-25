package org.argeo.server;

/**
 * Answer to a request to a remote service that sends back only one primitive
 */
public class PrimitiveAnswer {

	private Object primitive;

	/** Canonical constructor */
	public PrimitiveAnswer(Object primitive) {
		this.primitive = primitive;
	}

	public Object getValue() {
		return primitive;
	}

}
