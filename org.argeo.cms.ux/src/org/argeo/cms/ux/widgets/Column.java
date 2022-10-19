package org.argeo.cms.ux.widgets;

import org.argeo.api.cms.ux.CmsIcon;

public interface Column<T> {
	String getText(T model);

	default int getWidth() {
		return 200;
	}

	default CmsIcon getIcon(T model) {
		return null;
	}

}
