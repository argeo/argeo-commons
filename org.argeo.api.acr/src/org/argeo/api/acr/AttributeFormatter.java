package org.argeo.api.acr;

/**
 * An attribute type MUST consistently parse a string to an object so that
 * <code>parse(obj.toString()).equals(obj)</code> is verified.
 * {@link #format(Object)} can be overridden to provide more efficient
 * implementations but the returned
 * <code>String<code> MUST be the same, that is <code>format(obj).equals(obj.toString())</code>
 * is verified.
 */
public interface AttributeFormatter<T> {
	/** Parses a String to a Java object. */
	T parse(String str) throws IllegalArgumentException;

	/** Default implementation returns {@link Object#toString()} on the argument. */
	default String format(T obj) {
		return obj.toString();
	}
}
