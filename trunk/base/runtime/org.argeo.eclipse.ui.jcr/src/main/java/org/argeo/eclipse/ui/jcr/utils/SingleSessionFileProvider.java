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
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.specific.FileProvider;

/**
 * Implements a FileProvider for UI purposes. Unlike the
 * <code> JcrFileProvider </code>, it relies on a single session and manages
 * nodes with path only.
 * 
 * Note that considered id is the JCR path
 * 
 * Relies on common approach for JCR file handling implementation.
 * 
 * @author bsinou
 * 
 */

public class SingleSessionFileProvider implements FileProvider {

	private Session session;

	public SingleSessionFileProvider(Session session) {
		this.session = session;
	}

	public byte[] getByteArrayFileFromId(String fileId) {
		InputStream fis = null;
		byte[] ba = null;
		Node child = getFileNodeFromId(fileId);
		try {
			fis = (InputStream) child.getProperty(Property.JCR_DATA)
					.getBinary().getStream();
			ba = IOUtils.toByteArray(fis);

		} catch (Exception e) {
			throw new ArgeoException("Stream error while opening file", e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return ba;
	}

	public InputStream getInputStreamFromFileId(String fileId) {
		try {
			InputStream fis = null;

			Node child = getFileNodeFromId(fileId);
			fis = (InputStream) child.getProperty(Property.JCR_DATA)
					.getBinary().getStream();
			return fis;
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot get stream from file node for Id "
					+ fileId, re);
		}
	}

	/**
	 * 
	 * @param fileId
	 * @return Returns the child node of the nt:file node. It is the child node
	 *         that have the jcr:data property where actual file is stored.
	 *         never null
	 */
	private Node getFileNodeFromId(String fileId) {
		try {
			Node result = null;
			result = session.getNode(fileId);

			// Sanity checks
			if (result == null)
				throw new ArgeoException("File node not found for ID" + fileId);

			// Ensure that the node have the correct type.
			if (!result.isNodeType(NodeType.NT_FILE))
				throw new ArgeoException(
						"Cannot open file children Node that are not of "
								+ NodeType.NT_RESOURCE + " type.");

			Node child = result.getNodes().nextNode();
			if (child == null || !child.isNodeType(NodeType.NT_RESOURCE))
				throw new ArgeoException(
						"ERROR: IN the current implemented model, "
								+ NodeType.NT_FILE
								+ "  file node must have one and only one child of the nt:ressource, where actual data is stored");
			return child;
		} catch (RepositoryException re) {
			throw new ArgeoException("Erreur while getting file node of ID "
					+ fileId, re);
		}
	}
}
