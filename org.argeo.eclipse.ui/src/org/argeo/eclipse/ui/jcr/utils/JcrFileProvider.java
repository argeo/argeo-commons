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
package org.argeo.eclipse.ui.jcr.utils;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.FileProvider;

/**
 * Implements a FileProvider for UI purposes. Note that it might not be very
 * reliable as long as we have not fixed login and multi repository issues that
 * will be addressed in the next version.
 * 
 * NOTE: id used here is the real id of the JCR Node, not the JCR Path
 * 
 * Relies on common approach for JCR file handling implementation.
 * 
 */
@SuppressWarnings("deprecation")
public class JcrFileProvider implements FileProvider {

	// private Object[] rootNodes;
	private Node refNode;

	/**
	 * Must be set in order for the provider to be able to get current session
	 * and thus have the ability to get the file node corresponding to a given
	 * file ID
	 * 
	 * @param refNode
	 */
	public void setReferenceNode(Node refNode) {
		// FIXME : this introduces some concurrency ISSUES.
		this.refNode = refNode;
	}

	public byte[] getByteArrayFileFromId(String fileId) {
		InputStream fis = null;
		byte[] ba = null;
		Node child = getFileNodeFromId(fileId);
		try {
			fis = (InputStream) child.getProperty(Property.JCR_DATA).getBinary().getStream();
			ba = IOUtils.toByteArray(fis);

		} catch (Exception e) {
			throw new EclipseUiException("Stream error while opening file", e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return ba;
	}

	public InputStream getInputStreamFromFileId(String fileId) {
		try {
			InputStream fis = null;

			Node child = getFileNodeFromId(fileId);
			fis = (InputStream) child.getProperty(Property.JCR_DATA).getBinary().getStream();
			return fis;
		} catch (RepositoryException re) {
			throw new EclipseUiException("Cannot get stream from file node for Id " + fileId, re);
		}
	}

	/**
	 * Throws an exception if the node is not found in the current repository (a
	 * bit like a FileNotFoundException)
	 * 
	 * @param fileId
	 * @return Returns the child node of the nt:file node. It is the child node
	 *         that have the jcr:data property where actual file is stored.
	 *         never null
	 */
	private Node getFileNodeFromId(String fileId) {
		try {
			Node result = refNode.getSession().getNodeByIdentifier(fileId);

			// rootNodes: for (int j = 0; j < rootNodes.length; j++) {
			// // in case we have a classic JCR Node
			// if (rootNodes[j] instanceof Node) {
			// Node curNode = (Node) rootNodes[j];
			// if (result != null)
			// break rootNodes;
			// } // Case of a repository Node
			// else if (rootNodes[j] instanceof RepositoryNode) {
			// Object[] nodes = ((RepositoryNode) rootNodes[j])
			// .getChildren();
			// for (int i = 0; i < nodes.length; i++) {
			// Node node = (Node) nodes[i];
			// result = node.getSession().getNodeByIdentifier(fileId);
			// if (result != null)
			// break rootNodes;
			// }
			// }
			// }

			// Sanity checks
			if (result == null)
				throw new EclipseUiException("File node not found for ID" + fileId);

			Node child = null;

			boolean isValid = true;
			if (!result.isNodeType(NodeType.NT_FILE))
				// useless: mandatory child node
				// || !result.hasNode(Property.JCR_CONTENT))
				isValid = false;
			else {
				child = result.getNode(Property.JCR_CONTENT);
				if (!(child.isNodeType(NodeType.NT_RESOURCE) || child.hasProperty(Property.JCR_DATA)))
					isValid = false;
			}

			if (!isValid)
				throw new EclipseUiException("ERROR: In the current implemented model, '" + NodeType.NT_FILE
						+ "' file node must have a child node named jcr:content "
						+ "that has a BINARY Property named jcr:data " + "where the actual data is stored");
			return child;

		} catch (RepositoryException re) {
			throw new EclipseUiException("Erreur while getting file node of ID " + fileId, re);
		}
	}
}
