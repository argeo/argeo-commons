package org.argeo.eclipse.ui.jcr.util;

import java.util.Comparator;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;

/** Compares two JCR items (node or properties) based on their names. */
public class JcrItemsComparator implements Comparator<Item> {
	public int compare(Item o1, Item o2) {
		try {
			// TODO: put folder before files
			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot compare " + o1 + " and " + o2, e);
		}
	}

}
