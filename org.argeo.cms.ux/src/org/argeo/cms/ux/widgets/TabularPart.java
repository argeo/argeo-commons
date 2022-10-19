package org.argeo.cms.ux.widgets;

public interface TabularPart<INPUT, T> extends ColumnsPart<INPUT, T> {
	int getItemCount();

	T getData(int row);

	Column<T> getColumn(int index);

	int getColumnCount();
}
