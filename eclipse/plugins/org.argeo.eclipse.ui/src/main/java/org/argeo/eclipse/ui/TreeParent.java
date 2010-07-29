package org.argeo.eclipse.ui;

import java.util.ArrayList;
import java.util.List;

public class TreeParent extends TreeObject {
	private List<TreeObject> children;

	private boolean loaded;

	public TreeParent(String name) {
		super(name);
		children = new ArrayList<TreeObject>();
		loaded = false;
	}

	public synchronized void addChild(TreeObject child) {
		loaded = true;
		children.add(child);
		child.setParent(this);
	}

	public synchronized void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}

	public synchronized void clearChildren() {
		loaded = false;
		children.clear();
	}

	public synchronized TreeObject[] getChildren() {
		return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
	}

	public synchronized boolean hasChildren() {
		return children.size() > 0;
	}

	public synchronized Boolean isLoaded() {
		return loaded;
	}
}
