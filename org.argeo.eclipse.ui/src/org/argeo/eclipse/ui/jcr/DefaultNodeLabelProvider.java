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
package org.argeo.eclipse.ui.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Default label provider to manage node and corresponding UI objects. It
 * provides reasonable overwrite-able default for known JCR types.
 */
public class DefaultNodeLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 1216182332792151235L;

	public String getText(Object element) {
		try {
			if (element instanceof Node) {
				return getText((Node) element);
			} else if (element instanceof WrappedNode) {
				return getText(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getText(((NodesWrapper) element).getNode());
			}
			return super.getText(element);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get text for of " + element, e);
		}
	}

	protected String getText(Node node) throws RepositoryException {
		if (node.isNodeType(NodeType.MIX_TITLE)
				&& node.hasProperty(Property.JCR_TITLE))
			return node.getProperty(Property.JCR_TITLE).getString();
		else
			return node.getName();
	}

	@Override
	public Image getImage(Object element) {
		try {
			if (element instanceof Node) {
				return getImage((Node) element);
			} else if (element instanceof WrappedNode) {
				return getImage(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getImage(((NodesWrapper) element).getNode());
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve image for " + element, e);
		}
		return super.getImage(element);
	}

	protected Image getImage(Node node) throws RepositoryException {
		// FIXME who uses that?
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Node) {
				return getToolTipText((Node) element);
			} else if (element instanceof WrappedNode) {
				return getToolTipText(((WrappedNode) element).getNode());
			} else if (element instanceof NodesWrapper) {
				return getToolTipText(((NodesWrapper) element).getNode());
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get tooltip for " + element, e);
		}
		return super.getToolTipText(element);
	}

	protected String getToolTipText(Node node) throws RepositoryException {
		return null;
	}
}