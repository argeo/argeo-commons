package org.argeo.util.naming;

/**
 * A name which has been specified and for which an id has been defined
 * (typically an OID).
 */
public interface SpecifiedName {
	/** The name */
	String name();

	/** An RFC or the URLof some specification */
	default String getSpec() {
		return null;
	}

	/** Typically an OID */
	default String getID() {
		return getClass().getName() + "." + name();
	}
}
