package org.argeo.cms.ux.widgets;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractColumnsPart<INPUT, TYPE> extends AbstractDataPart<INPUT, TYPE> implements ColumnsPart<INPUT, TYPE> {

	private List<Column<TYPE>> columns = new ArrayList<>();

	@Override
	public Column<TYPE> getColumn(int index) {
		if (index >= columns.size())
			throw new IllegalArgumentException("There a only " + columns.size());
		return columns.get(index);
	}

	@Override
	public void addColumn(Column<TYPE> column) {
		columns.add(column);
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}
}
