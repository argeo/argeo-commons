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
package org.argeo.jcr.ui.explorer.browser;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.DefaultNodeLabelProvider;
import org.argeo.eclipse.ui.jcr.JcrImages;
import org.argeo.jcr.ui.explorer.model.RemoteRepositoryElem;
import org.argeo.jcr.ui.explorer.model.RepositoriesElem;
import org.argeo.jcr.ui.explorer.model.RepositoryElem;
import org.argeo.jcr.ui.explorer.model.SingleJcrNodeElem;
import org.argeo.jcr.ui.explorer.model.WorkspaceElem;
import org.eclipse.swt.graphics.Image;

public class NodeLabelProvider extends DefaultNodeLabelProvider {
	// Images

	public String getText(Object element) {
		try {
			if (element instanceof SingleJcrNodeElem) {
				SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
				return getText(sjn.getNode());
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
		} else if (element instanceof SingleJcrNodeElem)
			try {
				return super.getImage(((SingleJcrNodeElem) element).getNode());
			} catch (RepositoryException e) {
				return null;
			}
		return super.getImage(element);
	}

}
