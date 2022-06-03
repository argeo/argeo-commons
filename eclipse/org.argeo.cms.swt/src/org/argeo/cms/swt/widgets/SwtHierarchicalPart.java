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
public class SwtHierarchicalPart extends Composite implements HierarchicalPart {
	private static final long serialVersionUID = -2189742596757101778L;

	private final Tree tree;

	private Consumer<Object> onSelected;
	private Consumer<Object> onAction;

	public SwtHierarchicalPart(Composite parent, int style) {
		super(parent, style);
		setLayout(CmsSwtUtils.noSpaceGridLayout());
		tree = new Tree(this, SWT.VIRTUAL | SWT.BORDER);
	}

	public void refresh() {
		tree.addListener(SWT.SetData, event -> {
			TreeItem item = (TreeItem) event.item;
			TreeItem parentItem = item.getParentItem();
			if (parentItem == null) {
				refreshRootItem(item);
			} else {
				refreshItem(parentItem, parentItem);
			}
		});
		tree.setItemCount(getRootItemCount());
		CmsSwtUtils.fill(tree);

		tree.addSelectionListener(new SelectionListener() {

			private static final long serialVersionUID = 4334785560035009330L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				onSelected.accept(e.item.getData());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				onAction.accept(e.item.getData());
			}
		});

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

}
