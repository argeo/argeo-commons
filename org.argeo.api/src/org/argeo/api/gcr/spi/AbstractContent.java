package org.argeo.api.gcr.spi;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

import org.argeo.api.gcr.Content;

public abstract class AbstractContent extends AbstractMap<String, Object> implements Content {

	@Override
	public Set<Entry<String, Object>> entrySet() {
		Set<Entry<String, Object>> result = new HashSet<>();
		for (String key : keys()) {
			Entry<String, Object> entry = new Entry<String, Object>() {

				@Override
				public String getKey() {
					return key;
				}

				@Override
				public Object getValue() {
					// TODO check type
					return get(key, Object.class);
				}

				@Override
				public Object setValue(Object value) {
					throw new UnsupportedOperationException();
				}

			};
			result.add(entry);
		}
		return result;
	}

	protected abstract Iterable<String> keys();
	
	/*
	 * UTILITIES
	 */
	protected boolean isDefaultAttrTypeRequested(Class<?> clss) {
		// check whether clss is Object.class 
		return clss.isAssignableFrom(Object.class);
	}
}