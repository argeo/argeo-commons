package org.argeo.eclipse.ui;

public class TreeObject {
	private String name;
	private TreeParent parent;

	public TreeObject(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setParent(TreeParent parent) {
		this.parent = parent;
	}

	public TreeParent getParent() {
		return parent;
	}

	public String toString() {
		return getName();
	}
}
