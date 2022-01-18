package org.argeo.api.gcr.spi;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argeo.api.gcr.Content;

public abstract class AbstractContent extends AbstractMap<String, Object> implements Content {

	@Override
	public Set<Entry<String, Object>> entrySet() {
//		Set<Entry<String, Object>> result = new HashSet<>();
//		for (String key : keys()) {
//			Entry<String, Object> entry = new Entry<String, Object>() {
//
//				@Override
//				public String getKey() {
//					return key;
//				}
//
//				@Override
//				public Object getValue() {
//					return get(key, Object.class);
//				}
//
//				@Override
//				public Object setValue(Object value) {
//					throw new UnsupportedOperationException();
//				}
//
//			};
//			result.add(entry);
//		}
		Set<Entry<String, Object>> result = new AttrSet();
		return result;
	}

	protected abstract Iterable<String> keys();

	protected abstract void removeAttr(String key);

	@Override
	public String getPath() {
		List<Content> ancestors = new ArrayList<>();
		collectAncestors(ancestors, this);
		StringBuilder path = new StringBuilder();
		for (Content c : ancestors) {
			String name = c.getName();
			if (!"".equals(name))
				path.append('/').append(name);
		}
		return path.toString();
	}

	private void collectAncestors(List<Content> ancestors, Content content) {
		if (content == null)
			return;
		ancestors.add(0, content);
		collectAncestors(ancestors, content.getParent());
	}

	/*
	 * UTILITIES
	 */
	protected boolean isDefaultAttrTypeRequested(Class<?> clss) {
		// check whether clss is Object.class
		return clss.isAssignableFrom(Object.class);
	}

	@Override
	public String toString() {
		return "content "+getPath();
	}

	/*
	 * SUB CLASSES
	 */

	class AttrSet extends AbstractSet<Entry<String, Object>> {

		@Override
		public Iterator<Entry<String, Object>> iterator() {
			final Iterator<String> keys = keys().iterator();
			Iterator<Entry<String, Object>> it = new Iterator<Map.Entry<String, Object>>() {

				String key = null;

				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Entry<String, Object> next() {
					key = keys.next();
					// TODO check type
					Object value = get(key, Object.class);
					AbstractMap.SimpleEntry<String, Object> entry = new SimpleEntry<>(key, value);
					return entry;
				}

				@Override
				public void remove() {
					if (key != null) {
						AbstractContent.this.removeAttr(key);
					} else {
						throw new IllegalStateException("Iteration has not started");
					}
				}

			};
			return it;
		}

		@Override
		public int size() {
			int count = 0;
			for (String key : keys()) {
				count++;
			}
			return count;
		}

	}
}
