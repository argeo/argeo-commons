package org.argeo.cms.ux.widgets;

import java.util.List;

import org.argeo.api.cms.ux.CmsIcon;

public interface HierarchicalPart<T> extends ColumnsPart<T, T> {
	List<T> getChildren(T parent);

	String getText(T model);

	default CmsIcon getIcon(T model) {
		return null;
	}

}
