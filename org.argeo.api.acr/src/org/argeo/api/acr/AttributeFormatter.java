package org.argeo.api.acr;

import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.CrAttributeType.BooleanFormatter;

/**
 * An attribute type MUST consistently parse a string to an object so that
 * <code>parse(obj.toString()).equals(obj)</code> is verified.
 * {@link #format(Object)} can be overridden to provide more efficient
 * implementations but the returned <code>String</code> MUST be the same, that
 * is <code>format(obj).equals(obj.toString())</code> is verified.
 */
public interface AttributeFormatter<T> {
	/**
	 * Parses a {@link String} to a Java object.
	 * 
	 * @param str the {@link String} to parse or <code>null</code>
	 * @return the Java object or <code>null</code> if the argument was
	 *         <code>null</code> (except if of type {@link Boolean} in which case
	 *         {@link Boolean#FALSE} is returned, see {@link BooleanFormatter}).
	 */
	default T parse(String str) throws IllegalArgumentException {
		if (str == null)
			return null;
		return parse(RuntimeNamespaceContext.getNamespaceContext(), str);
	}

	/**
	 * Parses a String to a Java object, possibly using the namespace context to
	 * resolve QName or CURIE.
	 * 
	 * @param str the {@link String} to parse, cannot be <code>null</code>.
	 */
	T parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException;

	/** Default implementation returns {@link Object#toString()} on the argument. */
	default String format(T obj) {
		return obj.toString();
	}
}
