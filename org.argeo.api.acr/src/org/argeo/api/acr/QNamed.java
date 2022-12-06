package org.argeo.api.acr;

import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/** An optionally qualified name. Primarily meant to be used in enums. */
public interface QNamed extends Supplier<String> {
	String name();

	/** To be overridden when XML naming is not compatible with Java naming. */
	default String localName() {
		return name();
	}

	default QName qName() {
		return new ContentName(getNamespace(), localName(), getDefaultPrefix());
	}

	default String get(NamespaceContext namespaceContext) {
		return namespaceContext.getPrefix(getNamespace()) + ":" + localName();
	}

	default String get() {
		return getDefaultPrefix() + ":" + localName();
	}

	String getNamespace();

	String getDefaultPrefix();

	/** To be used by enums without namespace (typically XML attributes). */
	static interface Unqualified extends QNamed {
		@Override
		default String getNamespace() {
			return XMLConstants.NULL_NS_URI;
		}

		@Override
		default String getDefaultPrefix() {
			return XMLConstants.DEFAULT_NS_PREFIX;
		}

	}
}
