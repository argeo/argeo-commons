package org.argeo.api.acr;

import static org.argeo.api.acr.NamespaceUtils.unqualified;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

/** A {@link StructuredData} whose attributes have qualified keys. */
public interface QualifiedData<CHILD extends QualifiedData<CHILD>> extends StructuredData<QName, Object, CHILD> {
	QName getName();

	CHILD getParent();

	/*
	 * ATTRIBUTES OPERATIONS
	 */

	<A> Optional<A> get(QName key, Class<A> clss);

	Class<?> getType(QName key);

	boolean isMultiple(QName key);

	<A> List<A> getMultiple(QName key, Class<A> clss);

	/*
	 * PATH
	 */
	char getPathSeparator();

	/*
	 * ATTRIBUTES OPERATION HELPERS
	 */
	default boolean containsKey(QNamed key) {
		return containsKey(key.qName());
	}

	default <A> Optional<A> get(QNamed key, Class<A> clss) {
		return get(key.qName(), clss);
	}

	default Object get(QNamed key) {
		return get(key.qName());
	}

	default Object put(QNamed key, Object value) {
		return put(key.qName(), value);
	}

	default Object remove(QNamed key) {
		return remove(key.qName());
	}

	// TODO do we really need the helpers below?

	default Object get(String key) {
		return get(unqualified(key));
	}

	default Object put(String key, Object value) {
		return put(unqualified(key), value);
	}

	default Object remove(String key) {
		return remove(unqualified(key));
	}

	@SuppressWarnings("unchecked")
	default <A> List<A> getMultiple(QName key) {
		Class<A> type;
		try {
			type = (Class<A>) getType(key);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Requested type is not the default type");
		}
		List<A> res = getMultiple(key, type);
		return res;
	}

	/*
	 * CHILDREN
	 */

	default boolean hasChild(QName name) {
		for (CHILD child : this) {
			if (child.getName().equals(name))
				return true;
		}
		return false;
	}

	default boolean hasChild(QNamed name) {
		return hasChild(name.qName());
	}

	/** Any child with this name, or null if there is none */
	default CHILD anyChild(QName name) {
		for (CHILD child : this) {
			if (child.getName().equals(name))
				return child;
		}
		return null;
	}

	default List<CHILD> children(QName name) {
		List<CHILD> res = new ArrayList<>();
		for (CHILD child : this) {
			if (child.getName().equals(name))
				res.add(child);
		}
		return res;
	}

	default List<CHILD> children(QNamed name) {
		return children(name.qName());
	}

	default Optional<CHILD> soleChild(QNamed name) {
		return soleChild(name.qName());
	}

	default Optional<CHILD> soleChild(QName name) {
		List<CHILD> res = children(name);
		if (res.isEmpty())
			return Optional.empty();
		if (res.size() > 1)
			throw new IllegalStateException(this + " has multiple children with name " + name);
		return Optional.of(res.get(0));
	}

	default CHILD child(QName name) {
		return soleChild(name).orElseThrow();
	}

	default CHILD child(QNamed name) {
		return child(name.qName());
	}

	/*
	 * ATTR AS STRING
	 */
	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(QName key) {
		return get(key, String.class).orElse(null);
	}

	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(QNamed key) {
		return attr(key.qName());
	}

	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(String key) {
		return attr(unqualified(key));
	}

	/*
	 * SIBLINGS
	 */
	default int getSiblingIndex() {
		return 1;
	}
}
