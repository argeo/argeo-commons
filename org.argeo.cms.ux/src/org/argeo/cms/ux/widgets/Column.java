package org.argeo.cms.ux.widgets;

public interface Column<T> {
	String getText(T model);

	default int getWidth() {
		return 200;
	}
}
