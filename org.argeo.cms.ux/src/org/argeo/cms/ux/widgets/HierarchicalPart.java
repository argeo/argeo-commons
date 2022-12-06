package org.argeo.cms.ux.widgets;

import java.util.List;

/** A hierarchical representation of data. */
public interface HierarchicalPart<T> extends ColumnsPart<T, T> {
	List<T> getChildren(T parent);
}
