package org.argeo.api.tabular;


/** Write to a tabular content */
public interface TabularWriter {
	/** Append a new row of data */
	public void appendRow(Object[] row);

	/** Finish persisting data and release resources */
	public void close();
}
