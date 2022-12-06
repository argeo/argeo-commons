package org.argeo.cms.ux.widgets;

import org.argeo.api.cms.ux.CmsIcon;

/** A column in a data representation. */
@FunctionalInterface
public interface Column<TYPE> {
	String getText(TYPE model);

	default int getWidth() {
		return 200;
	}

	default CmsIcon getIcon(TYPE model) {
		return null;
	}

}
