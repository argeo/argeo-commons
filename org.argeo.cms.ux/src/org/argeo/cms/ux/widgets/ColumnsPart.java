package org.argeo.cms.ux.widgets;

/** A presentation of data in columns. */
public interface ColumnsPart<INPUT, TYPE> extends DataPart<INPUT, TYPE> {

	Column<TYPE> getColumn(int index);

	void addColumn(Column<TYPE> column);

	int getColumnCount();

}
