package org.argeo.cms.acr;

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
import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.util.LangUtils;

/** Partial reference implementation of a {@link ProvidedContent}. */
public abstract class AbstractContent extends AbstractMap<QName, Object> implements ProvidedContent {
	private final ProvidedSession session;

	// cache
//	private String _path = null;

	public AbstractContent(ProvidedSession session) {
		this.session = session;
	}

	/*
	 * ATTRIBUTES MAP IMPLEMENTATION
	 */
	@Override
	public Set<Entry<QName, Object>> entrySet() {
		Set<Entry<QName, Object>> result = new AttrSet();
		return result;
	}

	@Override
	public Object get(Object key) {
		return get((QName) key, Object.class).orElse(null);
	}

	/*
	 * ATTRIBUTES OPERATIONS
	 */
	@Override
	public Class<?> getType(QName key) {
		return String.class;
	}

	@Override
	public boolean isMultiple(QName key) {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> List<A> getMultiple(QName key, Class<A> clss) {
		Object value = get(key);
		if (value == null)
			return new ArrayList<>();
		if (value instanceof List) {
			if (clss.isAssignableFrom(Object.class))
				return (List<A>) value;
			List<A> res = new ArrayList<>();
			List<?> lst = (List<?>) value;
			for (Object o : lst) {
				A item = CrAttributeType.cast(clss, o).get();
				res.add(item);
			}
			return res;
		} else {// singleton
			A res = CrAttributeType.cast(clss, value).get();
			return Collections.singletonList(res);
		}
	}

	/*
	 * CONTENT OPERATIONS
	 */

	@Override
	public String getPath() {
//		if (_path != null)
//			return _path;
		List<Content> ancestors = new ArrayList<>();
		collectAncestors(ancestors, this);
		StringBuilder path = new StringBuilder();
		ancestors: for (Content c : ancestors) {
			QName name = c.getName();
			if (CrName.root.qName().equals(name))
				continue ancestors;

			path.append('/');
			path.append(NamespaceUtils.toPrefixedName(name));
			int siblingIndex = c.getSiblingIndex();
			if (siblingIndex != 1)
				path.append('[').append(siblingIndex).append(']');
		}
//		_path = path.toString();
//		return _path;
		return path.toString();
	}

	private void collectAncestors(List<Content> ancestors, Content content) {
		if (content == null)
			return;
		ancestors.add(0, content);
		collectAncestors(ancestors, content.getParent());
	}

	@Override
	public int getDepth() {
		List<Content> ancestors = new ArrayList<>();
		collectAncestors(ancestors, this);
		return ancestors.size();
	}

	@Override
	public boolean isRoot() {
		return CrName.root.qName().equals(getName());
	}

	@Override
	public String getSessionLocalId() {
		return getPath();
	}

	/*
	 * SESSION
	 */

	@Override
	public ProvidedSession getSession() {
		return session;
	}

	/*
	 * TYPING
	 */

	@Override
	public List<QName> getContentClasses() {
		return new ArrayList<>();
	}

	/*
	 * UTILITIES
	 */
//	protected boolean isDefaultAttrTypeRequested(Class<?> clss) {
//		// check whether clss is Object.class
//		return clss.isAssignableFrom(Object.class);
//	}

//	@Override
//	public String toString() {
//		return "content " + getPath();
//	}

	/*
	 * DEFAULTS
	 */
	// - no children
	// - no attributes
	// - cannot be modified
	@Override
	public Iterator<Content> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public Content add(QName name, QName... classes) {
		throw new UnsupportedOperationException("Content cannot be added.");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Content cannot be removed.");
	}

	protected Iterable<QName> keys() {
		return Collections.emptySet();
	}

	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		return Optional.empty();
	}

	protected void removeAttr(QName key) {
		throw new UnsupportedOperationException("Attributes cannot be removed.");
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
			return LangUtils.size(keys());
		}

	}

	/*
	 * OBJECT METHODS
	 */
	@Override
	public String toString() {
		return "content " + getPath();
	}
}
