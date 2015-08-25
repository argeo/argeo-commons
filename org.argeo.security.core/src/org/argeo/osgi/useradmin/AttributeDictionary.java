package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

class AttributeDictionary extends Dictionary<String,Object> {
	private final Attributes attributes;
	private final List<String> effectiveKeys = new ArrayList<String>();
	private final List<String> attrFilter;
	private final Boolean includeFilter;

	public AttributeDictionary(Attributes attributes, List<String> attrFilter,
			Boolean includeFilter) {
		this.attributes = attributes;
		this.attrFilter = attrFilter;
		this.includeFilter = includeFilter;
		try {
			NamingEnumeration<String> ids = attributes.getIDs();
			while (ids.hasMore()) {
				String id = ids.next();
				if (includeFilter && attrFilter.contains(id))
					effectiveKeys.add(id);
				else if (!includeFilter && !attrFilter.contains(id))
					effectiveKeys.add(id);
			}
		} catch (NamingException e) {
			throw new ArgeoUserAdminException(
					"Cannot initialise attribute dictionary", e);
		}
	}

	@Override
	public int size() {
		return effectiveKeys.size();
	}

	@Override
	public boolean isEmpty() {
		return effectiveKeys.size() == 0;
	}

	@Override
	public Enumeration<String> keys() {
		return Collections.enumeration(effectiveKeys);
	}

	@Override
	public Enumeration<Object> elements() {
		final Iterator<String> it = effectiveKeys.iterator();
		return new Enumeration<Object>() {

			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public Object nextElement() {
				String key = it.next();
				try {
					return attributes.get(key).get();
				} catch (NamingException e) {
					throw new ArgeoUserAdminException(
							"Cannot get value for key " + key, e);
				}
			}

		};
	}

	@Override
	public Object get(Object key) {
		try {
			Attribute attr = attributes.get(key.toString());
			if (attr == null)
				return null;
			return attr.get();
		} catch (NamingException e) {
			throw new ArgeoUserAdminException("Cannot get value for attribute "
					+ key, e);
		}
	}

	@Override
	public Object put(String key, Object value) {
		if (!(value instanceof String || value instanceof byte[]))
			throw new IllegalArgumentException("Value must be String or byte[]");

		if (includeFilter && !attrFilter.contains(key))
			throw new IllegalArgumentException("Key " + key + " not included");
		else if (!includeFilter && attrFilter.contains(key))
			throw new IllegalArgumentException("Key " + key + " excluded");

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
		if (includeFilter && !attrFilter.contains(key))
			throw new IllegalArgumentException("Key " + key + " not included");
		else if (!includeFilter && attrFilter.contains(key))
			throw new IllegalArgumentException("Key " + key + " excluded");

		try {
			Attribute attr = attributes.remove(key.toString());
			if (attr != null)
				return attr.get();
			else
				return null;
		} catch (NamingException e) {
			throw new ArgeoUserAdminException("Cannot remove attribute " + key,
					e);
		}
	}
}
