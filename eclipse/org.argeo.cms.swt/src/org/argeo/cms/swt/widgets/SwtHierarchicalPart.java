package org.argeo.cms.swt.widgets;

import java.util.List;

import org.argeo.api.cms.ux.CmsIcon;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.HierarchicalPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/** {@link HierarchicalPart} implementation based on a {@link Tree}. */
public class SwtHierarchicalPart<T> extends AbstractSwtPart<T, T> {
	private static final long serialVersionUID = -6247710601465713047L;

	private final Tree tree;

	private HierarchicalPart<T> hierarchicalPart;

	public SwtHierarchicalPart(Composite parent, int style, HierarchicalPart<T> hierarchicalPart) {
		super(parent, style, hierarchicalPart);
		tree = new Tree(this, SWT.BORDER);
		tree.setLayoutData(CmsSwtUtils.fillAll());
		this.hierarchicalPart = hierarchicalPart;

		tree.addSelectionListener(selectionListener);
	}

	@Override
	public void refresh() {
		// TODO optimise
		// tree.clearAll(true);

		for (TreeItem rootItem : tree.getItems()) {
			rootItem.dispose();
		}

		List<T> rootItems = hierarchicalPart.getChildren(hierarchicalPart.getInput());
		for (T child : rootItems) {
			TreeItem childItem = addTreeItem(null, child);
//			List<T> grandChildren = hierarchicalPart.getChildren(child);
//			for (T grandChild : grandChildren) {
//				addTreeItem(childItem, grandChild);
//			}
		}
//		tree.addListener(SWT.SetData, event -> {
//			TreeItem item = (TreeItem) event.item;
//			TreeItem parentItem = item.getParentItem();
//			if (parentItem == null) {
//				refreshRootItem(item);
//			} else {
//				refreshItem(parentItem, item);
//			}
//		});
//		tree.setItemCount(getRootItemCount());

		tree.addListener(SWT.Expand, event -> {
			final TreeItem root = (TreeItem) event.item;
			TreeItem[] items = root.getItems();
			for (TreeItem item : items) {
				if (item.getData() != null) {
//					List<T> grandChildren = hierarchicalPart.getChildren((T) item.getData());
//					for (T grandChild : grandChildren) {
//						addTreeItem(item, grandChild);
//					}
					return;
				}
				item.dispose();
			}

			List<T> children = hierarchicalPart.getChildren((T) root.getData());
			for (T child : children) {
				TreeItem childItem = addTreeItem(root, child);
//				List<T> grandChildren = hierarchicalPart.getChildren(child);
//				for (T grandChild : grandChildren) {
//					addTreeItem(childItem, grandChild);
//				}
			}
		});

		CmsSwtUtils.fill(tree);

	}

	protected TreeItem addTreeItem(TreeItem parent, T data) {
		TreeItem item = parent == null ? new TreeItem(tree, SWT.NONE) : new TreeItem(parent, SWT.NONE);
		item.setData(data);
		String txt = hierarchicalPart.getText(data);
		if (txt != null)
			item.setText(hierarchicalPart.getText(data));
		CmsIcon icon = hierarchicalPart.getIcon(data);
		// TODO optimize
		List<T> grandChildren = hierarchicalPart.getChildren(data);
		if (grandChildren.size() != 0)
			new TreeItem(item, SWT.NONE);
		return item;
//if(icon!=null)
//	item.setImage(null);
	}

	protected Tree getTree() {
		return tree;
	}

}
