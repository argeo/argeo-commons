package org.argeo.cms.internal.useradmin;

import java.util.Dictionary;
import java.util.Enumeration;

import org.argeo.cms.CmsException;

/** Empty for the time being */
class JcrRoleProperties extends Dictionary<String, Object> {

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Enumeration<String> keys() {
		return new KeyEnumeration();
	}

	@Override
	public Enumeration<Object> elements() {
		return new ValueEnumeration();
	}

	@Override
	public Object get(Object key) {
		return null;
	}

	@Override
	public Object put(String key, Object value) {
		throw new CmsException("Not implemented yet");
	}

	@Override
	public Object remove(Object key) {
		return null;
	}

	private class KeyEnumeration implements Enumeration<String> {

		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public String nextElement() {
			return null;
		}

	}

	private class ValueEnumeration implements Enumeration<Object> {

		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public Object nextElement() {
			return null;
		}

	}
}
