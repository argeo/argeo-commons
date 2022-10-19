package org.argeo.cms.ux.widgets;

import java.util.ArrayList;
import java.util.List;

public class DefaultTabularPart<INPUT, T> extends AbstractTabularPart<INPUT, T> implements TabularPart<INPUT, T> {
	private List<T> content;

	@Override
	public int getItemCount() {
		return content.size();
	}

	@Override
	public T getData(int row) {
		assert row < getItemCount();
		return content.get(row);
	}

	@Override
	public void refresh() {
		INPUT input = getInput();
		if (input == null) {
			content = new ArrayList<>();
			return;
		}
		content = asList(input);
		super.refresh();
	}

	protected List<T> asList(INPUT input) {
		List<T> res = new ArrayList<>();
		content.clear();
		if (input instanceof List) {
			content = (List<T>) input;
		} else if (input instanceof Iterable) {
			for (T item : (Iterable<T>) input)
				content.add(item);
		} else {
			throw new IllegalArgumentException(
					"Unsupported class " + input.getClass() + ", method should be overridden.");
		}
		return res;
	}

}
