package org.argeo.cms.swt.widgets;

import java.util.function.Consumer;

import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.TabularPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** {@link TabularPart} implementation based on a {@link Table}. */
public class SwtTabularPart implements TabularPart {
	private Composite area;

	private final Table table;

	private Consumer<Object> onSelected;
	private Consumer<Object> onAction;

	public SwtTabularPart(Composite parent, int style) {
		area = new Composite(parent, style);
		area.setLayout(CmsSwtUtils.noSpaceGridLayout());
		table = new Table(area, SWT.VIRTUAL | SWT.BORDER);
	}

	@Override
	public void refresh() {
		// TODO optimise
		table.clearAll();
		table.addListener(SWT.SetData, event -> {
			TableItem item = (TableItem) event.item;
			refreshItem(item);
		});
		table.setItemCount(getItemCount());
		CmsSwtUtils.fill(table);

		table.addSelectionListener(new SelectionListener() {
			private static final long serialVersionUID = -5225905921522775948L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (onSelected != null)
					onSelected.accept(e.item.getData());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (onAction != null)
					onAction.accept(e.item.getData());
			}
		});

	}

	@Override
	public void setInput(Object data) {
		area.setData(data);
		refresh();
	}

	@Override
	public Object getInput() {
		return area.getData();
	}

	protected void refreshItem(TableItem item) {

	}

	protected int getItemCount() {
		return 0;
	}

	protected Table getTable() {
		return table;
	}

	public void onSelected(Consumer<Object> onSelected) {
		this.onSelected = onSelected;
	}

	public void onAction(Consumer<Object> onAction) {
		this.onAction = onAction;
	}

}
