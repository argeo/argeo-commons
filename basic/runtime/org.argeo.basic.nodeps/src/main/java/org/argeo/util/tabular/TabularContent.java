package org.argeo.util.tabular;

import java.util.List;

/**
 * Content organized as a table, possibly with headers. Only JCR types are
 * supported even though there is not direct dependency on JCR.
 */
public interface TabularContent {
	/** The headers of this table or <code>null</code> is none available. */
	public List<String> getHeaders();
}
