package org.argeo.util.tabular;

import java.util.Iterator;

/** Navigation of rows */
public interface TabularRowIterator extends Iterator<TabularRow> {
	/**
	 * Current line number, incremented by each call to next(), starts at 0, but
	 * will therefore be 1 for the first row returned.
	 */
	public Long getCurrentLineNumber();
}
