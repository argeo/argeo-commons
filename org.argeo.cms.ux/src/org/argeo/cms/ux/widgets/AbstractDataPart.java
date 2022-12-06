package org.argeo.cms.ux.widgets;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

public abstract class AbstractDataPart<INPUT, TYPE> implements DataPart<INPUT, TYPE> {
	private Consumer<TYPE> onSelected;
	private Consumer<TYPE> onAction;

	private IdentityHashMap<DataView<INPUT, TYPE>, Object> views = new IdentityHashMap<>();

	private INPUT data;

	@Override
	public void setInput(INPUT data) {
		this.data = data;
		refresh();
	}

	@Override
	public INPUT getInput() {
		return data;
	}

	@Override
	public void onSelected(Consumer<TYPE> onSelected) {
		this.onSelected = onSelected;
	}

	@Override
	public void onAction(Consumer<TYPE> onAction) {
		this.onAction = onAction;
	}

	public Consumer<TYPE> getOnSelected() {
		return onSelected;
	}

	public Consumer<TYPE> getOnAction() {
		return onAction;
	}

	@Override
	public void refresh() {
		for (DataView<INPUT, TYPE> view : views.keySet()) {
			view.refresh();
		}
	}

	protected void notifyItemCountChange() {
		for (DataView<INPUT, TYPE> view : views.keySet()) {
			view.notifyItemCountChange();
		}
	}

	@Override
	public void addView(DataView<INPUT, TYPE> view) {
		views.put(view, new Object());
	}

	@Override
	public void removeView(DataView<INPUT, TYPE> view) {
		views.remove(view);
	}
}
