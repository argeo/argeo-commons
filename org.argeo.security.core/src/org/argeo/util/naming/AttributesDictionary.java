package org.argeo.util.naming;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

public class AttributesDictionary extends Dictionary<String, Object> {
	private final Attributes attributes;

	/** The provided attributes is wrapped, not copied. */
	public AttributesDictionary(Attributes attributes) {
		if (attributes == null)
			throw new IllegalArgumentException("Attributes cannot be null");
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
		NamingEnumeration<String> namingEnumeration = attributes.getIDs();
		return new Enumeration<String>() {

			@Override
			public boolean hasMoreElements() {
				return namingEnumeration.hasMoreElements();
			}

			@Override
			public String nextElement() {
				return namingEnumeration.nextElement();
			}

		};
	}

	@Override
	public Enumeration<Object> elements() {
		NamingEnumeration<String> namingEnumeration = attributes.getIDs();
		return new Enumeration<Object>() {

			@Override
			public boolean hasMoreElements() {
				return namingEnumeration.hasMoreElements();
			}

			@Override
			public Object nextElement() {
				String key = namingEnumeration.nextElement();
				return get(key);
			}

		};
	}

	@Override
	/** @returns a <code>String</code> or <code>String[]</code> */
	public Object get(Object key) {
		try {
			if (key == null)
				throw new IllegalArgumentException("Key cannot be null");
			Attribute attr = attributes.get(key.toString());
			if (attr == null)
				return null;
			if (attr.size() == 0)
				throw new IllegalStateException("There must be at least one value");
			else if (attr.size() == 1) {
				return attr.get().toString();
			} else {// multiple
				String[] res = new String[attr.size()];
				for (int i = 0; i < attr.size(); i++) {
					Object value = attr.get();
					if (value == null)
						throw new RuntimeException("Values cannot be null");
					res[i] = attr.get(i).toString();
				}
				return res;
			}
		} catch (NamingException e) {
			throw new RuntimeException("Cannot get value for " + key, e);
		}
	}

	@Override
	public Object put(String key, Object value) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		if (value == null)
			throw new IllegalArgumentException("Value cannot be null");

		Object oldValue = get(key);
		Attribute attr = attributes.get(key);
		if (attr == null) {
			attr = new BasicAttribute(key);
			attributes.put(attr);
		}

		if (value instanceof String[]) {
			String[] values = (String[]) value;
			// clean additional values
			for (int i = values.length; i < attr.size(); i++)
				attr.remove(i);
			// set values
			for (int i = 0; i < values.length; i++) {
				attr.set(i, values[i]);
			}
		} else {
			if (attr.size() != 1)
				throw new IllegalArgumentException("Attribute " + key + " is multi-valued");
			attr.set(0, value.toString());
		}
		return oldValue;
	}

	@Override
	public Object remove(Object key) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		Object oldValue = get(key);
		if (oldValue == null)
			return null;
		return attributes.remove(key.toString());
	}

	/**
	 * Copy the <b>content</b> of an {@link javax.naming.Attributes} to the
	 * provided {@link Dictionary}.
	 */
	public static void copy(Attributes attributes, Dictionary<String, Object> dictionary) {
		AttributesDictionary ad = new AttributesDictionary(attributes);
		Enumeration<String> keys = ad.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			dictionary.put(key, ad.get(key));
		}
	}

	/**
	 * Copy a {@link Dictionary} into an {@link javax.naming.Attributes}.
	 */
	public static void copy(Dictionary<String, Object> dictionary, Attributes attributes) {
		AttributesDictionary ad = new AttributesDictionary(attributes);
		Enumeration<String> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			ad.put(key, dictionary.get(key));
		}
	}
}
