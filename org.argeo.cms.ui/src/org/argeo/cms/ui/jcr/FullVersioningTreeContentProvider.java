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
package org.argeo.cms.ui.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Display some version information of a JCR full versionable node in a tree
 * like structure
 */
public class FullVersioningTreeContentProvider implements ITreeContentProvider {
	private static final long serialVersionUID = 8691772509491211112L;

	/**
	 * Sends back the first level of the Tree. input element must be a single
	 * node object
	 */
	public Object[] getElements(Object inputElement) {
		try {
			Node rootNode = (Node) inputElement;
			String curPath = rootNode.getPath();
			VersionManager vm = rootNode.getSession().getWorkspace()
					.getVersionManager();

			VersionHistory vh = vm.getVersionHistory(curPath);
			List<Version> result = new ArrayList<Version>();
			VersionIterator vi = vh.getAllLinearVersions();

			while (vi.hasNext()) {
				result.add(vi.nextVersion());
			}
			return result.toArray();
		} catch (RepositoryException re) {
			throw new EclipseUiException(
					"Unexpected error while getting version elements", re);
		}
	}

	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof Version) {
				List<Node> tmp = new ArrayList<Node>();
				tmp.add(((Version) parentElement).getFrozenNode());
				return tmp.toArray();
			}
		} catch (RepositoryException re) {
			throw new EclipseUiException("Unexpected error while getting child "
					+ "node for version element", re);
		}
		return null;
	}

	public Object getParent(Object element) {
		try {
			// this will not work in a simpleVersionning environment, parent is
			// not a node.
			if (element instanceof Node
					&& ((Node) element).isNodeType(NodeType.NT_FROZEN_NODE)) {
				Node node = (Node) element;
				return node.getParent();
			} else
				return null;
		} catch (RepositoryException e) {
			return null;
		}
	}

	public boolean hasChildren(Object element) {
		try {
			if (element instanceof Version)
				return true;
			else if (element instanceof Node)
				return ((Node) element).hasNodes();
			else
				return false;
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot check children of " + element, e);
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
