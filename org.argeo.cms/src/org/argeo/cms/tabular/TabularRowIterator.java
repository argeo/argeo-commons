package org.argeo.cms.tabular;

import java.util.Iterator;

/** Navigation of rows */
public interface TabularRowIterator extends Iterator<TabularRow> {
	/**
	 * Current row number, has to be incremented by each call to next() ; starts at 0, will
	 * therefore be 1 for the first row returned.
	 */
	public Long getCurrentRowNumber();
}
