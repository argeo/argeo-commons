package org.argeo.cms.internal;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

@Deprecated
class JcrContentProvider implements ITreeContentProvider {
	private static final long serialVersionUID = -1333678161322488674L;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null)
			return;
		if (!(newInput instanceof Node))
			throw new CmsException("Input " + newInput + " must be a node");
	}

	@Override
	public Object[] getElements(Object inputElement) {
		try {
			Node node = (Node) inputElement;
			ArrayList<Node> arr = new ArrayList<Node>();
			NodeIterator nit = node.getNodes();
			while (nit.hasNext()) {
				arr.add(nit.nextNode());
			}
			return arr.toArray();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get elements", e);
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		try {
			Node node = (Node) parentElement;
			ArrayList<Node> arr = new ArrayList<Node>();
			NodeIterator nit = node.getNodes();
			while (nit.hasNext()) {
				arr.add(nit.nextNode());
			}
			return arr.toArray();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get elements", e);
		}
	}

	@Override
	public Object getParent(Object element) {
		try {
			Node node = (Node) element;
			if (node.getName().equals(""))
				return null;
			else
				return node.getParent();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get elements", e);
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		try {
			Node node = (Node) element;
			return node.hasNodes();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get elements", e);
		}
	}

}
