package org.argeo.osgi.useradmin;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

class AttributeDictionary extends Dictionary {
	private final Attributes attributes;

	public AttributeDictionary(Attributes attributes) {
		this.attributes = attributes;
	}

	@Override
	public int size() {
		return attributes.size();
	}

	@Override
	public boolean isEmpty() {
		return attributes.size() == 0;
	}

	@Override
	public Enumeration<String> keys() {
		return attributes.getIDs();
	}

	@Override
	public Enumeration<Object> elements() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(Object key) {
		try {
			return attributes.get(key.toString()).get();
		} catch (NamingException e) {
			throw new ArgeoUserAdminException("Cannot get value for attribute "
					+ key, e);
		}
	}

	@Override
	public Object put(Object key, Object value) {
		if (!(value instanceof String || value instanceof byte[]))
			throw new IllegalArgumentException(
					"Value muste be String or byte[]");
		try {
			Attribute attribute = attributes.get(key.toString());
			attribute = new BasicAttribute(key.toString());
			attribute.add(value);
			Attribute previousAttribute = attributes.put(attribute);
			if (previousAttribute != null)
				return previousAttribute.get();
			else
				return null;
		} catch (NamingException e) {
			throw new ArgeoUserAdminException("Cannot get value for attribute "
					+ key, e);
		}
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

}
