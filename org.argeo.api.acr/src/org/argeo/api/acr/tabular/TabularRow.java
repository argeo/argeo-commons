package org.argeo.api.acr.tabular;

/** A row of tabular data */
public interface TabularRow {
	/** The value at this column index */
	public Object get(Integer col);

	/** The raw objects (direct references) */
	public Object[] toArray();

	/** Number of columns */
	public int size();
}
