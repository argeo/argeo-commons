package org.argeo.cms.ux.widgets;

import java.util.function.Consumer;

public interface DataPart<INPUT, T> {
	void setInput(INPUT data);

	INPUT getInput();

	void onSelected(Consumer<T> onSelected);

	Consumer<T> getOnSelected();

	void onAction(Consumer<T> onAction);

	Consumer<T> getOnAction();

	void refresh();

	void addView(DataView<INPUT, T> view);

	void removeView(DataView<INPUT, T> view);
}
