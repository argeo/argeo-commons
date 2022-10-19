package org.argeo.cms.swt.widgets;

import java.util.List;

import org.argeo.api.cms.ux.CmsIcon;
import org.argeo.cms.swt.CmsSwtTheme;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.widgets.HierarchicalPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/** View of a {@link HierarchicalPart} based on a {@link Tree}. */
public class SwtTreeView<T> extends AbstractSwtView<T, T> {
	private static final long serialVersionUID = -6247710601465713047L;

	private final Tree tree;

	private HierarchicalPart<T> hierarchicalPart;
	private CmsSwtTheme theme;

	public SwtTreeView(Composite parent, int style, HierarchicalPart<T> hierarchicalPart) {
		super(parent, style, hierarchicalPart);
		theme = CmsSwtUtils.getCmsTheme(parent);

		tree = new Tree(this, SWT.BORDER);
		tree.setLayoutData(CmsSwtUtils.fillAll());
		this.hierarchicalPart = hierarchicalPart;

		tree.addSelectionListener(selectionListener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void refresh() {
		// TODO optimise
		for (TreeItem rootItem : tree.getItems()) {
			rootItem.dispose();
		}

		List<T> rootItems = hierarchicalPart.getChildren(hierarchicalPart.getInput());
		for (T child : rootItems) {
			addTreeItem(null, child);
		}

		tree.addListener(SWT.Expand, event -> {
			final TreeItem root = (TreeItem) event.item;
			TreeItem[] items = root.getItems();
			for (TreeItem item : items) {
				if (item.getData() != null) {
					return;
				}
				item.dispose();
			}

			List<T> children = hierarchicalPart.getChildren((T) root.getData());
			for (T child : children) {
				addTreeItem(root, child);
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
		if (icon != null) {
			Image image = theme.getSmallIcon(icon);
			item.setImage(image);
		}
		// TODO optimize
		List<T> grandChildren = hierarchicalPart.getChildren(data);
		if (grandChildren.size() != 0)
			new TreeItem(item, SWT.NONE);
		return item;
	}

	protected Tree getTree() {
		return tree;
	}

}
