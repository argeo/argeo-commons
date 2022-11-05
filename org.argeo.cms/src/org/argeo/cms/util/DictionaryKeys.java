package org.argeo.cms.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Access the keys of a {@link String}-keyed {@link Dictionary} (common throughout
 * the OSGi APIs) as an {@link Iterable} so that they are easily usable in
 * for-each loops.
 */
class DictionaryKeys implements Iterable<String> {
	private final Dictionary<String, ?> dictionary;

	public DictionaryKeys(Dictionary<String, ?> dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	public Iterator<String> iterator() {
		return new KeyIterator(dictionary.keys());
	}

	private static class KeyIterator implements Iterator<String> {
		private final Enumeration<String> keys;

		KeyIterator(Enumeration<String> keys) {
			this.keys = keys;
		}

		@Override
		public boolean hasNext() {
			return keys.hasMoreElements();
		}

		@Override
		public String next() {
			return keys.nextElement();
		}

	}
}
