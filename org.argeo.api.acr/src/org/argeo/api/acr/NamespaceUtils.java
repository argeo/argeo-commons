package org.argeo.api.acr;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/** Static utilities around namespaces and prefixes. */
public class NamespaceUtils {
	/**
	 * A {@link Comparator} ordering by namespace (full URI) and then local part.
	 */
	public final static Comparator<QName> QNAME_COMPARATOR = new Comparator<QName>() {

		@Override
		public int compare(QName qn1, QName qn2) {
			if (Objects.equals(qn1.getNamespaceURI(), qn2.getNamespaceURI())) {// same namespace
				return qn1.getLocalPart().compareTo(qn2.getLocalPart());
			} else {
				return qn1.getNamespaceURI().compareTo(qn2.getNamespaceURI());
			}
		}

	};

	/**
	 * Return a {@link ContentName} from a prefixed name, using the default runtime
	 * {@link NamespaceContext}.
	 * 
	 * @see RuntimeNamespaceContext#getNamespaceContext()
	 */
	public static ContentName parsePrefixedName(String nameWithPrefix) {
		return parsePrefixedName(RuntimeNamespaceContext.getNamespaceContext(), nameWithPrefix);
	}

	/**
	 * Return a {@link ContentName} from a prefixed name, using the provided
	 * {@link NamespaceContext}. Since {@link QName#QName(String, String)} does not
	 * validate, it can conceptually parse a CURIE.
	 * 
	 * @see https://en.wikipedia.org/wiki/CURIE
	 */
	public static ContentName parsePrefixedName(NamespaceContext nameSpaceContext, String nameWithPrefix) {
		Objects.requireNonNull(nameWithPrefix, "Name cannot be null");
		if (nameWithPrefix.charAt(0) == '{') {
			return new ContentName(QName.valueOf(nameWithPrefix), nameSpaceContext);
		}
		int index = nameWithPrefix.indexOf(':');
		if (index < 0) {
			return new ContentName(nameWithPrefix);
		}
		String prefix = nameWithPrefix.substring(0, index);
		// TODO deal with empty name?
		String localName = nameWithPrefix.substring(index + 1);
		String namespaceURI = nameSpaceContext.getNamespaceURI(prefix);
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			throw new IllegalArgumentException("Prefix " + prefix + " is unbound.");
		return new ContentName(namespaceURI, localName, prefix);
	}

	/**
	 * The prefixed name of this {@link QName}, using the default runtime
	 * {@link NamespaceContext}.
	 * 
	 * @see RuntimeNamespaceContext#getNamespaceContext()
	 */
	public static String toPrefixedName(QName name) {
		return toPrefixedName(RuntimeNamespaceContext.getNamespaceContext(), name);
	}

	/**
	 * The prefixed name of this {@link QName}, using the provided
	 * {@link NamespaceContext}.
	 */
	public static String toPrefixedName(NamespaceContext nameSpaceContext, QName name) {
		if (XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()))
			return name.getLocalPart();
		String prefix = nameSpaceContext.getPrefix(name.getNamespaceURI());
		if (prefix == null)
			throw new IllegalStateException("Namespace " + name.getNamespaceURI() + " is unbound.");
		return prefix + ":" + name.getLocalPart();
	}

	/**
	 * Whether thei {@link QName} has a namespace, that is its namespace is not
	 * {@link XMLConstants#NULL_NS_URI}.
	 */
	public static boolean hasNamespace(QName qName) {
		return !qName.getNamespaceURI().equals(XMLConstants.NULL_NS_URI);
	}

	/** Throws an exception if the provided string has a prefix. */
	public static void checkNoPrefix(String unqualified) throws IllegalArgumentException {
		if (unqualified.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + unqualified + " has a prefix");
	}

	/**
	 * Create an unqualified {@link QName}, checking that the argument does not
	 * contain a prefix.
	 */
	public static QName unqualified(String name) {
		checkNoPrefix(name);
		return new ContentName(XMLConstants.NULL_NS_URI, name, XMLConstants.DEFAULT_NS_PREFIX);

	}

	/**
	 * The common (fully qualified) representation of this name, as defined in
	 * {@link QName#toString()}. This should be used when a fully qualified
	 * representation is required, as subclasses of {@link QName} may override the
	 * {@link QName#toString()} method.
	 * 
	 * @see ContentName#toString()
	 */
	public static String toFullyQualified(QName name) {
		if (name.getNamespaceURI().equals(XMLConstants.NULL_NS_URI)) {
			return name.getLocalPart();
		} else {
			return "{" + name.getNamespaceURI() + "}" + name.getLocalPart();
		}

	}

	/*
	 * STANDARD NAMESPACE CONTEXT OPERATIONS as specified in NamespaceContext
	 */
	/**
	 * The standard prefix for well known namespaces as defined in
	 * {@link NamespaceContext}, or null if the namespace is not well-known. Helper
	 * method simplifying the implementation of a {@link NamespaceContext}.
	 * 
	 * @see NamespaceContext#getPrefix(String)
	 */
	public static String getStandardPrefix(String namespaceURI) {
		if (namespaceURI == null)
			throw new IllegalArgumentException("Namespace URI cannot be null");
		if (XMLConstants.XML_NS_URI.equals(namespaceURI))
			return XMLConstants.XML_NS_PREFIX;
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI))
			return XMLConstants.XMLNS_ATTRIBUTE;
		return null;
	}

	/**
	 * The standard prefixes for well known namespaces as defined in
	 * {@link NamespaceContext}, or null if the namespace is not well-known. Helper
	 * method simplifying the implementation of a {@link NamespaceContext}.
	 * 
	 * @see NamespaceContext#getPrefixes(String)
	 */
	public static Iterator<String> getStandardPrefixes(String namespaceURI) {
		String prefix = getStandardPrefix(namespaceURI);
		if (prefix == null)
			return null;
		return Collections.singleton(prefix).iterator();
	}

	/**
	 * The standard URI for well known prefixes as defined in
	 * {@link NamespaceContext}, or null if the prefix is not well-known. Helper
	 * method simplifying the implementation of a {@link NamespaceContext}.
	 * 
	 * @see NamespaceContext#getNamespaceURI(String)
	 */
	public static String getStandardNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException("Prefix cannot be null");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix))
			return XMLConstants.XML_NS_URI;
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		return null;
	}

	/**
	 * The namespace URI for a given prefix, based on the provided mapping and the
	 * standard prefixes/URIs.
	 * 
	 * @return the namespace URI for this prefix, or
	 *         {@link XMLConstants#NULL_NS_URI} if unknown.
	 */
	public static String getNamespaceURI(Function<String, String> mapping, String prefix) {
		String namespaceURI = NamespaceUtils.getStandardNamespaceURI(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))
			return XMLConstants.NULL_NS_URI;
		namespaceURI = mapping.apply(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		return XMLConstants.NULL_NS_URI;
	}

	/**
	 * The prefix for a given namespace URI, based on the provided mapping and the
	 * standard prefixes/URIs.
	 * 
	 * @return the prefix for this namespace URI. What is returned or throws as
	 *         exception if the prefix is not found depdnds on the mapping function.
	 */
	public static String getPrefix(Function<String, String> mapping, String namespaceURI) {
		String prefix = NamespaceUtils.getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return XMLConstants.DEFAULT_NS_PREFIX;
		return mapping.apply(namespaceURI);
	}

	/**
	 * The prefixes for a given namespace URI, based on the provided mapping and the
	 * standard prefixes/URIs.
	 * 
	 * @return the prefixes for this namespace URI. What is returned or throws as
	 *         exception if the prefix is not found depdnds on the mapping function.
	 */
	public static Iterator<String> getPrefixes(Function<String, Set<String>> mapping, String namespaceURI) {
		Iterator<String> standard = NamespaceUtils.getStandardPrefixes(namespaceURI);
		if (standard != null)
			return standard;
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX).iterator();
		Set<String> prefixes = mapping.apply(namespaceURI);
		assert prefixes != null;
		return prefixes.iterator();
	}

	/** singleton */
	private NamespaceUtils() {
	}

}
