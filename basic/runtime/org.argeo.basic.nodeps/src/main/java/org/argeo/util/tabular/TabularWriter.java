package org.argeo.util.tabular;

import java.util.List;

/** Write to a tabular content */
public interface TabularWriter {
	/** Append a new row of data */
	public void appendRow(List<?> row);

	/** Finish persisting data and release resources */
	public void close();
}
