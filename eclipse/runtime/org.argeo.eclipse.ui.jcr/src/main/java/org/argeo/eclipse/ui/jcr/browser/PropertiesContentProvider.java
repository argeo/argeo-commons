package org.argeo.eclipse.ui.jcr.browser;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PropertiesContentProvider implements IStructuredContentProvider {
	private ItemComparator itemComparator = new ItemComparator();

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		try {
			if (inputElement instanceof Node) {
				Set<Property> props = new TreeSet<Property>(itemComparator);
				PropertyIterator pit = ((Node) inputElement).getProperties();
				while (pit.hasNext())
					props.add(pit.nextProperty());
				return props.toArray();
			}
			return new Object[] {};
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get element for " + inputElement,
					e);
		}
	}

}
