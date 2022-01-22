package org.argeo.api.acr.spi;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.CrName;

public abstract class AbstractContent extends AbstractMap<QName, Object> implements Content {

	/*
	 * ATTRIBUTES OPERATIONS
	 */
	protected abstract Iterable<QName> keys();

	protected abstract void removeAttr(QName key);

	@Override
	public Set<Entry<QName, Object>> entrySet() {
		Set<Entry<QName, Object>> result = new AttrSet();
		return result;
	}

	@Override
	public Class<?> getType(QName key) {
		return String.class;
	}

	@Override
	public boolean isMultiple(QName key) {
		return false;
	}

	@Override
	public <A> Optional<List<A>> getMultiple(QName key, Class<A> clss) {
		Object value = get(key);
		if (value == null)
			return null;
		if (value instanceof List) {
			try {
				List<A> res = (List<A>) value;
				return Optional.of(res);
			} catch (ClassCastException e) {
				List<A> res = new ArrayList<>();
				List<?> lst = (List<?>) value;
				try {
					for (Object o : lst) {
						A item = (A) o;
						res.add(item);
					}
					return Optional.of(res);
				} catch (ClassCastException e1) {
					return Optional.empty();
				}
			}
		} else {// singleton
			try {
				A res = (A) value;
				return Optional.of(Collections.singletonList(res));
			} catch (ClassCastException e) {
				return Optional.empty();
			}
		}
	}

	/*
	 * CONTENT OPERATIONS
	 */

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
					Optional<?> value = get(key, Object.class);
					assert !value.isEmpty();
					AbstractMap.SimpleEntry<QName, Object> entry = new SimpleEntry<>(key, value.get());
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
