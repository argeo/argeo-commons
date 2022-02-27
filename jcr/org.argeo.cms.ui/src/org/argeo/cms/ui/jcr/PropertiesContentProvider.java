package org.argeo.cms.ui.jcr;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.jcr.util.JcrItemsComparator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** Simple content provider that displays all properties of a given Node */
public class PropertiesContentProvider implements IStructuredContentProvider {
	private static final long serialVersionUID = 5227554668841613078L;
	private JcrItemsComparator itemComparator = new JcrItemsComparator();

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
			throw new EclipseUiException("Cannot get element for "
					+ inputElement, e);
		}
	}
}
