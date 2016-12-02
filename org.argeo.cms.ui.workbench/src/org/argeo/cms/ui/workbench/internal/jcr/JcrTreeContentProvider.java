/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.ui.workbench.internal.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.jcr.utils.JcrItemsComparator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Implementation of the {@code ITreeContentProvider} in order to display a
 * single JCR node and its children in a tree like structure
 */
public class JcrTreeContentProvider implements ITreeContentProvider {
	private static final long serialVersionUID = -2128326504754297297L;
	// private Node rootNode;
	private JcrItemsComparator itemComparator = new JcrItemsComparator();

	/**
	 * Sends back the first level of the Tree. input element must be a single
	 * node object
	 */
	public Object[] getElements(Object inputElement) {
		try {
			Node rootNode = (Node) inputElement;
			List<Node> result = new ArrayList<Node>();
			NodeIterator ni = rootNode.getNodes();
			while (ni.hasNext())
				result.add(ni.nextNode());
			return result.toArray();
		} catch (RepositoryException re) {
			throw new EclipseUiException("Unable to retrieve elements for " + inputElement, re);
		}
	}

	public Object[] getChildren(Object parentElement) {
		return childrenNodes((Node) parentElement);
	}

	public Object getParent(Object element) {
		try {
			Node node = (Node) element;
			if (!node.getPath().equals("/"))
				return node.getParent();
			else
				return null;
		} catch (RepositoryException e) {
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		try {
			return ((Node) element).hasNodes();
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot check children existence on " + element, e);
		}
	}

	protected Object[] childrenNodes(Node parentNode) {
		try {
			List<Node> children = new ArrayList<Node>();
			NodeIterator nit = parentNode.getNodes();
			while (nit.hasNext())
				children.add(nit.nextNode());
			Node[] arr = children.toArray(new Node[0]);
			Arrays.sort(arr, itemComparator);
			return arr;
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot list children of " + parentNode, e);
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
