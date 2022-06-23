package org.argeo.cms.swt.widgets;

import java.util.function.Consumer;

import org.argeo.api.cms.ux.CmsIcon;
import org.argeo.cms.swt.CmsSwtTheme;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.Column;
import org.argeo.cms.ux.widgets.TabularPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** {@link TabularPart} implementation based on a {@link Table}. */
public class SwtTabularPart implements TabularPart {
	private Composite area;

	private final Table table;

	private Consumer<Object> onSelected;
	private Consumer<Object> onAction;

	private CmsSwtTheme theme;

	public SwtTabularPart(Composite parent, int style) {
		theme = CmsSwtUtils.getCmsTheme(parent);
		area = new Composite(parent, style);
		area.setLayout(CmsSwtUtils.noSpaceGridLayout());
		table = new Table(area, SWT.VIRTUAL | SWT.BORDER);
		table.setLinesVisible(true);
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
					onSelected.accept(getDataFromEvent(e));
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (onAction != null)
					onAction.accept(getDataFromEvent(e));
			}
		});

	}

	protected Object getDataFromEvent(SelectionEvent e) {
		Object data = e.item.getData();
		if (data == null)
			data = getData(getTable().indexOf((TableItem) e.item));
		return data;
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
		int row = getTable().indexOf(item);
		for (int i = 0; i < item.getParent().getColumnCount(); i++) {
			Column<Object> column = (Column<Object>) item.getParent().getColumn(i).getData();
			Object data = getData(row);
			item.setData(data);
			String text = data != null ? column.getText(data) : "";
			if (text != null)
				item.setText(i, text);
			CmsIcon icon = column.getIcon(data);
			if (icon != null) {
				Image image = theme.getSmallIcon(icon);
				item.setImage(i, image);
			}
		}
	}

	protected int getItemCount() {
		return 0;
	}

	protected Object getData(int row) {
		return null;
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

	@Override
	public void addColumn(Column<?> column) {
		TableColumn swtColumn = new TableColumn(table, SWT.NONE);
		swtColumn.setWidth(column.getWidth());
		swtColumn.setData(column);

	}

	public Composite getArea() {
		return area;
	}

}
