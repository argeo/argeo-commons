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

	/**
	 * A {@link QName} corresponding to this definition. Calls
	 * {@link #createQName()} by default, but it could return a cached value.
	 */
	default QName qName() {
		return createQName();
	}

	/**
	 * A prefixed representation of this qualified name within the provided
	 * {@link NamespaceContext}.
	 */
	default String get(NamespaceContext namespaceContext) {
		return namespaceContext.getPrefix(getNamespace()) + ":" + localName();
	}

	/**
	 * Create a {@link QName} corresponding on this definition. Can typically be
	 * used to cache the {@link QName} in enums.
	 */
	default QName createQName() {
		return new ContentName(getNamespace(), localName(), getDefaultPrefix());
	}

	/**
	 * This qualified named with its default prefix. If it is unqualified this
	 * method should be overridden, or QNamed.Unqualified be used.
	 */
	default String get() {
		return getDefaultPrefix() + ":" + localName();
	}

	/** The namespace URI of this qualified name. */
	String getNamespace();

	/**
	 * The default prefix of this qualified name, as expected to be found in
	 * {@link RuntimeNamespaceContext}.
	 */
	String getDefaultPrefix();

	/** Compares to a plain {@link QName}. */
	default boolean equals(QName qName) {
		return qName().equals(qName);
	}

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

		@Override
		default String get() {
			return localName();
		}

	}
}
