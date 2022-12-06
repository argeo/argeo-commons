package org.argeo.cms.ux.widgets;

/** A tabular presentation of data. */
public interface TabularPart<INPUT, TYPE> extends ColumnsPart<INPUT, TYPE> {
	int getItemCount();

	TYPE getData(int row);
}
