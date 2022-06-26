package org.argeo.cms.ux.widgets;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

public abstract class AbstractDataPart<INPUT, T> implements DataPart<INPUT, T> {

	private Consumer<T> onSelected;
	private Consumer<T> onAction;

	private IdentityHashMap<DataView<INPUT, T>, Object> views = new IdentityHashMap<>();

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
	public void onSelected(Consumer<T> onSelected) {
		this.onSelected = onSelected;
	}

	@Override
	public void onAction(Consumer<T> onAction) {
		this.onAction = onAction;
	}

	public Consumer<T> getOnSelected() {
		return onSelected;
	}

	public Consumer<T> getOnAction() {
		return onAction;
	}

	@Override
	public void refresh() {
		for (DataView<INPUT, T> view : views.keySet()) {
			view.refresh();
		}
	}

	@Override
	public void addView(DataView<INPUT, T> view) {
		views.put(view, new Object());
	}

	@Override
	public void removeView(DataView<INPUT, T> view) {
		views.remove(view);
	}

}
