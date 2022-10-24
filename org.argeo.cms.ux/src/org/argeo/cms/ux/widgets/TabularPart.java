package org.argeo.cms.ux.widgets;

/** A tabular presentation of data. */
public interface TabularPart<INPUT, T> extends ColumnsPart<INPUT, T> {
	int getItemCount();

	T getData(int row);

	Column<T> getColumn(int index);

	void addColumn(Column<T> column);

	int getColumnCount();
}
