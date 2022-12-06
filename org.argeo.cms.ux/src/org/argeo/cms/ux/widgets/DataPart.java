package org.argeo.cms.ux.widgets;

import java.util.function.Consumer;

public interface DataPart<INPUT, TYPE> {
	void setInput(INPUT data);

	INPUT getInput();

	void onSelected(Consumer<TYPE> onSelected);

	Consumer<TYPE> getOnSelected();

	void onAction(Consumer<TYPE> onAction);

	Consumer<TYPE> getOnAction();

	void refresh();

	void addView(DataView<INPUT, TYPE> view);

	void removeView(DataView<INPUT, TYPE> view);

//	void select(TYPE data);
//
//	TYPE getSelected();

}
