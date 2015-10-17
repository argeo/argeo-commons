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
package org.argeo.eclipse.ui.workbench.jcr.internal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.jcr.JcrImages;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.RemoteRepositoryElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.RepositoriesElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.RepositoryElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.SingleJcrNodeElem;
import org.argeo.eclipse.ui.workbench.jcr.internal.model.WorkspaceElem;
import org.argeo.jcr.ArgeoTypes;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/** Provides reasonable defaults for know JCR types. */
public class NodeLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = -3662051696443321843L;

	private final static Log log = LogFactory.getLog(NodeLabelProvider.class);

	public String getText(Object element) {
		try {
			if (element instanceof SingleJcrNodeElem) {
				SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
				return getText(sjn.getNode());
			} else if (element instanceof Node) {
				return getText((Node) element);
			} else
				return super.getText(element);
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected JCR error while getting node name.");
		}
	}

	protected String getText(Node node) throws RepositoryException {
		String label = node.getName();
		StringBuffer mixins = new StringBuffer("");
		for (NodeType type : node.getMixinNodeTypes())
			mixins.append(' ').append(type.getName());

		return label + " [" + node.getPrimaryNodeType().getName() + mixins
				+ "]";
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof RemoteRepositoryElem) {
			if (((RemoteRepositoryElem) element).isConnected())
				return JcrImages.REMOTE_CONNECTED;
			else
				return JcrImages.REMOTE_DISCONNECTED;
		} else if (element instanceof RepositoryElem) {
			if (((RepositoryElem) element).isConnected())
				return JcrImages.REPOSITORY_CONNECTED;
			else
				return JcrImages.REPOSITORY_DISCONNECTED;
		} else if (element instanceof WorkspaceElem) {
			if (((WorkspaceElem) element).isConnected())
				return JcrImages.WORKSPACE_CONNECTED;
			else
				return JcrImages.WORKSPACE_DISCONNECTED;
		} else if (element instanceof RepositoriesElem) {
			return JcrImages.REPOSITORIES;
		} else if (element instanceof SingleJcrNodeElem) {
			Node nodeElem = ((SingleJcrNodeElem) element).getNode();
			return getImage(nodeElem);

			// if (element instanceof Node) {
			// return getImage((Node) element);
			// } else if (element instanceof WrappedNode) {
			// return getImage(((WrappedNode) element).getNode());
			// } else if (element instanceof NodesWrapper) {
			// return getImage(((NodesWrapper) element).getNode());
			// }
		}
		// try {
		// return super.getImage();
		// } catch (RepositoryException e) {
		// return null;
		// }
		return super.getImage(element);
	}

	protected Image getImage(Node node) {
		try {
			if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
				return JcrImages.FILE;
			else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER))
				return JcrImages.FOLDER;
			else if (node.getPrimaryNodeType().isNodeType(NodeType.NT_RESOURCE))
				return JcrImages.BINARY;
			else if (node.isNodeType(ArgeoTypes.ARGEO_USER_HOME))
				return JcrImages.HOME;
			else
				return JcrImages.NODE;
		} catch (RepositoryException e) {
			log.warn("Error while retrieving type for " + node
					+ " in order to display corresponding image");
			e.printStackTrace();
			return null;
		}

	}

}
