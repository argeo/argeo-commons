package org.argeo.cms.swt.widgets;

import java.util.function.Consumer;

import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.HierarchicalPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/** {@link HierarchicalPart} implementation based on a {@link Tree}. */
public class SwtHierarchicalPart implements HierarchicalPart {
	private Composite area;
	private final Tree tree;

	private Consumer<Object> onSelected;
	private Consumer<Object> onAction;

	public SwtHierarchicalPart(Composite parent, int style) {
		area = new Composite(parent, style);
		area.setLayout(CmsSwtUtils.noSpaceGridLayout());
		tree = new Tree(area, SWT.VIRTUAL | SWT.BORDER);
	}

	@Override
	public void refresh() {
		// TODO optimise
		tree.clearAll(true);
		tree.addListener(SWT.SetData, event -> {
			TreeItem item = (TreeItem) event.item;
			TreeItem parentItem = item.getParentItem();
			if (parentItem == null) {
				refreshRootItem(item);
			} else {
				refreshItem(parentItem, item);
			}
		});
		tree.setItemCount(getRootItemCount());
		CmsSwtUtils.fill(tree);

		tree.addSelectionListener(new SelectionListener() {

			private static final long serialVersionUID = 4334785560035009330L;

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

	protected void refreshRootItem(TreeItem item) {

	}

	protected void refreshItem(TreeItem parentItem, TreeItem item) {

	}

	protected int getRootItemCount() {
		return 0;
	}

	protected Tree getTree() {
		return tree;
	}

	public void onSelected(Consumer<Object> onSelected) {
		this.onSelected = onSelected;
	}

	public void onAction(Consumer<Object> onAction) {
		this.onAction = onAction;
	}

	public Composite getArea() {
		return area;
	}

}
