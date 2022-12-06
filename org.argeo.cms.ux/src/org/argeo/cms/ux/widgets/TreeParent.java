package org.argeo.cms.ux.widgets;

import java.util.ArrayList;
import java.util.List;

/** Parent / children semantic to be used for simple UI Tree structure */
public class TreeParent {
	private String name;
	private TreeParent parent;

	private List<Object> children;

	/**
	 * Unique id within the context of a tree display. If set, equals() and
	 * hashCode() methods will be based on it
	 */
	private String path = null;

	/** False until at least one child has been added, then true until cleared */
	private boolean loaded = false;

	public TreeParent(String name) {
		this.name = name;
		children = new ArrayList<Object>();
	}

	public synchronized void addChild(Object child) {
		loaded = true;
		children.add(child);
		if (child instanceof TreeParent)
			((TreeParent) child).setParent(this);
	}

	/**
	 * Remove this child. The child is disposed.
	 */
	public synchronized void removeChild(Object child) {
		children.remove(child);
		if (child instanceof TreeParent) {
			((TreeParent) child).dispose();
		}
	}

	public synchronized void clearChildren() {
		for (Object obj : children) {
			if (obj instanceof TreeParent)
				((TreeParent) obj).dispose();
		}
		loaded = false;
		children.clear();
	}

	/**
	 * If overridden, <code>super.dispose()</code> must be called, typically
	 * after custom cleaning.
	 */
	public synchronized void dispose() {
		clearChildren();
		parent = null;
		children = null;
	}

	public synchronized Object[] getChildren() {
		return children.toArray(new Object[children.size()]);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> List<T> getChildrenOfType(Class<T> clss) {
		List<T> lst = new ArrayList<T>();
		for (Object obj : children) {
			if (clss.isAssignableFrom(obj.getClass()))
				lst.add((T) obj);
		}
		return lst;
	}

	public synchronized boolean hasChildren() {
		return children.size() > 0;
	}

	public Object getChildByName(String name) {
		for (Object child : children) {
			if (child.toString().equals(name))
				return child;
		}
		return null;
	}

	public synchronized Boolean isLoaded() {
		return loaded;
	}

	public String getName() {
		return name;
	}

	public void setParent(TreeParent parent) {
		this.parent = parent;
		if (parent != null && parent.path != null)
			this.path = parent.path + '/' + name;
		else
			this.path = '/' + name;
	}

	public TreeParent getParent() {
		return parent;
	}

	public String toString() {
		return getName();
	}

	public int compareTo(TreeParent o) {
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode() {
		if (path != null)
			return path.hashCode();
		else
			return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (path != null && obj instanceof TreeParent)
			return path.equals(((TreeParent) obj).path);
		else
			return name.equals(obj.toString());
	}

}
