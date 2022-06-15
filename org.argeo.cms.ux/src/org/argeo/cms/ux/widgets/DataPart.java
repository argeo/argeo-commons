package org.argeo.cms.ux.widgets;

import java.util.function.Consumer;

public interface DataPart {
	void setInput(Object data);

	Object getInput();

	void refresh();

	void onSelected(Consumer<Object> onSelected);

	void onAction(Consumer<Object> onAction);
}
