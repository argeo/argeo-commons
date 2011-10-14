package org.argeo.jcr.ui.explorer.utils;

import java.util.Comparator;

import org.argeo.eclipse.ui.TreeParent;

public class TreeObjectsComparator implements Comparator<TreeParent> {
	public int compare(TreeParent o1, TreeParent o2) {
		return o1.getName().compareTo(o2.getName());
	}
}