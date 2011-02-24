package org.argeo.eclipse.ui.jcr.browser;

import java.util.Comparator;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;

public class ItemComparator implements Comparator<Item> {
	public int compare(Item o1, Item o2) {
		try {
			// TODO: put folder before files
			return o1.getName().compareTo(o2.getName());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot compare " + o1 + " and " + o2, e);
		}
	}

}
