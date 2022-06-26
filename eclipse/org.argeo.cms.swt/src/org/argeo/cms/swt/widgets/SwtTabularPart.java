package org.argeo.cms.swt.widgets;

import org.argeo.api.cms.ux.CmsIcon;
import org.argeo.cms.swt.CmsSwtTheme;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.Column;
import org.argeo.cms.ux.widgets.TabularPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** {@link TabularPart} implementation based on a {@link Table}. */
public class SwtTabularPart<INPUT, T> extends AbstractSwtPart<INPUT, T> {
	private static final long serialVersionUID = -1114155772446357750L;
	private final Table table;
	private TabularPart<INPUT, T> tabularPart;

	private CmsSwtTheme theme;

	public SwtTabularPart(Composite parent, int style, TabularPart<INPUT, T> tabularPart) {
		super(parent, style, tabularPart);
		theme = CmsSwtUtils.getCmsTheme(parent);

		table = new Table(this, SWT.VIRTUAL | SWT.BORDER);
		table.setLinesVisible(true);
		table.setLayoutData(CmsSwtUtils.fillAll());

		this.tabularPart = tabularPart;
	}

	@Override
	public void refresh() {
		// TODO optimise
		table.clearAll();
		table.addListener(SWT.SetData, event -> {
			TableItem item = (TableItem) event.item;
			refreshItem(item);
		});
		table.setItemCount(tabularPart.getItemCount());
		for (int i = 0; i < tabularPart.getColumnCount(); i++) {
			TableColumn swtColumn = new TableColumn(table, SWT.NONE);
			swtColumn.setWidth(tabularPart.getColumn(i).getWidth());
		}
		CmsSwtUtils.fill(table);

		table.addSelectionListener(selectionListener);

	}

	protected Object getDataFromEvent(SelectionEvent e) {
		Object data = e.item.getData();
		if (data == null)
			data = tabularPart.getData(getTable().indexOf((TableItem) e.item));
		return data;
	}

	protected void refreshItem(TableItem item) {
		int row = getTable().indexOf(item);
		for (int i = 0; i < tabularPart.getColumnCount(); i++) {
			Column<T> column = tabularPart.getColumn(i);
			T data = tabularPart.getData(row);
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

	protected Table getTable() {
		return table;
	}

}
