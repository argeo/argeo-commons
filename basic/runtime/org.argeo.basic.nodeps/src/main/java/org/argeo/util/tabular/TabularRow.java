package org.argeo.util.tabular;

/** A row of tabular data */
public interface TabularRow {
	public Object get(Integer col);

	public int size();
}
