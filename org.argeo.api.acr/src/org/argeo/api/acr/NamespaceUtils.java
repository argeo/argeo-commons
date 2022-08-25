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

public class NamespaceUtils {

	public static ContentName parsePrefixedName(String nameWithPrefix) {
		return parsePrefixedName(RuntimeNamespaceContext.getNamespaceContext(), nameWithPrefix);
	}

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
			throw new IllegalStateException("Prefix " + prefix + " is unbound.");
		return new ContentName(namespaceURI, localName, prefix);
	}

	public static String toPrefixedName(QName name) {
		return toPrefixedName(RuntimeNamespaceContext.getNamespaceContext(), name);
	}

	public static String toPrefixedName(NamespaceContext nameSpaceContext, QName name) {
		if (XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()))
			return name.getLocalPart();
		String prefix = nameSpaceContext.getPrefix(name.getNamespaceURI());
		if (prefix == null)
			throw new IllegalStateException("Namespace " + name.getNamespaceURI() + " is unbound.");
		return prefix + ":" + name.getLocalPart();
	}

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

	public static boolean hasNamespace(QName qName) {
		return !qName.getNamespaceURI().equals(XMLConstants.NULL_NS_URI);
	}

	/*
	 * DEFAULT NAMESPACE CONTEXT OPERATIONS as specified in NamespaceContext
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

	public static Iterator<String> getStandardPrefixes(String namespaceURI) {
		String prefix = getStandardPrefix(namespaceURI);
		if (prefix == null)
			return null;
		return Collections.singleton(prefix).iterator();
	}

	public static String getStandardNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException("Prefix cannot be null");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix))
			return XMLConstants.XML_NS_URI;
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		return null;
	}

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

	public static String getPrefix(Function<String, String> mapping, String namespaceURI) {
		String prefix = NamespaceUtils.getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return XMLConstants.DEFAULT_NS_PREFIX;
		return mapping.apply(namespaceURI);
	}

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
