package org.argeo.cms.ux.widgets;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTabularPart<INPUT, T> extends AbstractDataPart<INPUT, T> implements TabularPart<INPUT, T> {

	private List<Column<T>> columns = new ArrayList<>();

	@Override
	public Column<T> getColumn(int index) {
		if (index >= columns.size())
			throw new IllegalArgumentException("There a only " + columns.size());
		return columns.get(index);
	}

	@Override
	public void addColumn(Column<T> column) {
		columns.add(column);
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}
}
