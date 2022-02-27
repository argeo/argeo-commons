package org.argeo.api.acr;

import java.util.Locale;
import java.util.Objects;

import javax.security.auth.Subject;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public interface ContentSession extends NamespaceContext {
	Subject getSubject();

	Locale getLocale();

	Content get(String path);

	/*
	 * NAMESPACE CONTEXT
	 */

	default ContentName parsePrefixedName(String nameWithPrefix) {
		Objects.requireNonNull(nameWithPrefix, "Name cannot be null");
		if (nameWithPrefix.charAt(0) == '{') {
			return new ContentName(QName.valueOf(nameWithPrefix), this);
		}
		int index = nameWithPrefix.indexOf(':');
		if (index < 0) {
			return new ContentName(nameWithPrefix);
		}
		String prefix = nameWithPrefix.substring(0, index);
		// TODO deal with empty name?
		String localName = nameWithPrefix.substring(index + 1);
		String namespaceURI = getNamespaceURI(prefix);
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			throw new IllegalStateException("Prefix " + prefix + " is unbound.");
		return new ContentName(namespaceURI, localName, prefix);
	}

	default String toPrefixedName(QName name) {
		if (XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()))
			return name.getLocalPart();
		String prefix = getPrefix(name.getNamespaceURI());
		if (prefix == null)
			throw new IllegalStateException("Namespace " + name.getNamespaceURI() + " is unbound.");
		return prefix + ":" + name.getLocalPart();
	}

}
