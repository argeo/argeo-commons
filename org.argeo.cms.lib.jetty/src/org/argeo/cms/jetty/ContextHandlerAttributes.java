package org.argeo.cms.jetty;

import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * A {@link Map} implementation wrapping the attributes of a Jetty
 * {@link ContextHandler}.
 */
class ContextHandlerAttributes extends AbstractMap<String, Object> {
	private ContextHandler contextHandler;

	public ContextHandlerAttributes(ContextHandler contextHandler) {
		super();
		this.contextHandler = contextHandler;
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		Set<Entry<String, Object>> entries = new HashSet<>();
		for (Enumeration<String> keys = contextHandler.getAttributeNames(); keys.hasMoreElements();) {
			entries.add(new ContextAttributeEntry(keys.nextElement()));
		}
		return entries;
	}

	@Override
	public Object put(String key, Object value) {
		Object previousValue = get(key);
		contextHandler.setAttribute(key, value);
		return previousValue;
	}

	private class ContextAttributeEntry implements Map.Entry<String, Object> {
		private final String key;

		public ContextAttributeEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return contextHandler.getAttribute(key);
		}

		@Override
		public Object setValue(Object value) {
			Object previousValue = getValue();
			contextHandler.setAttribute(key, value);
			return previousValue;
		}

	}
}
