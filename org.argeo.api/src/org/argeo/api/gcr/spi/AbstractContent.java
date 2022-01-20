package org.argeo.api.gcr.spi;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.CrName;

public abstract class AbstractContent extends AbstractMap<QName, Object> implements Content {

	@Override
	public Set<Entry<QName, Object>> entrySet() {
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
		Set<Entry<QName, Object>> result = new AttrSet();
		return result;
	}

	protected abstract Iterable<QName> keys();

	protected abstract void removeAttr(QName key);

	@Override
	public String getPath() {
		List<Content> ancestors = new ArrayList<>();
		collectAncestors(ancestors, this);
		StringBuilder path = new StringBuilder();
		for (Content c : ancestors) {
			QName name = c.getName();
			// FIXME
			if (!CrName.ROOT.get().equals(name))
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
		return "content " + getPath();
	}

	/*
	 * SUB CLASSES
	 */

	class AttrSet extends AbstractSet<Entry<QName, Object>> {

		@Override
		public Iterator<Entry<QName, Object>> iterator() {
			final Iterator<QName> keys = keys().iterator();
			Iterator<Entry<QName, Object>> it = new Iterator<Map.Entry<QName, Object>>() {

				QName key = null;

				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Entry<QName, Object> next() {
					key = keys.next();
					// TODO check type
					Object value = get(key, Object.class);
					AbstractMap.SimpleEntry<QName, Object> entry = new SimpleEntry<>(key, value);
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
			for (QName key : keys()) {
				count++;
			}
			return count;
		}

	}
}
