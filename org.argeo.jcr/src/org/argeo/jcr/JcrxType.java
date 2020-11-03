package org.argeo.jcr;

/** Node types declared by the JCR extensions. */
public interface JcrxType {
	/**
	 * Node type for an XML value, which will be serialized in XML as an element
	 * containing text.
	 */
	public final static String JCRX_XMLVALUE = "{http://www.argeo.org/ns/jcrx}xmlvalue";

	/** Node type for the node containing the text. */
	public final static String JCRX_XMLTEXT = "{http://www.argeo.org/ns/jcrx}xmltext";

	/** Mixin node type for a set of checksums. */
	public final static String JCRX_CSUM = "{http://www.argeo.org/ns/jcrx}csum";

}
