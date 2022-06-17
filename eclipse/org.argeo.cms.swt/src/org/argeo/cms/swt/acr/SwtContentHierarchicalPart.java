package org.argeo.cms.swt.acr;

import java.util.Iterator;

import org.argeo.api.acr.Content;
import org.argeo.cms.swt.widgets.SwtHierarchicalPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

public class SwtContentHierarchicalPart extends SwtHierarchicalPart {

	public SwtContentHierarchicalPart(Composite parent, int style) {
		super(parent, style);
	}

	public Content getContent() {
		return (Content) getInput();
	}

	@Override
	protected void refreshRootItem(TreeItem item) {
		refreshItem(null, item);
	}

	@Override
	protected void refreshItem(TreeItem parentItem, TreeItem item) {
		int index = getTree().indexOf(item);
		Content parentContent = parentItem == null ? getContent() : (Content) parentItem.getData();
		Content content = null;
		int count = 0;
		children: for (Content c : parentContent) {
			if (count == index) {
				content = c;
				break children;
			}
			count++;
		}
		item.setData(content);
		item.setText(content.getName().toString());
		item.setItemCount(getChildrenCount(content));
	}

	@Override
	protected int getRootItemCount() {
		return getChildrenCount(getContent());
	}

	static int getChildrenCount(Content content) {
		int count = 0;
		for (Iterator<Content> it = content.iterator(); it.hasNext();it.next()) {
			count++;
		}
		return count;
	}
}
