package org.argeo.eclipse.ui.jcr.utils;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.browser.RepositoryNode;
import org.argeo.eclipse.ui.jcr.browser.WorkspaceNode;
import org.argeo.eclipse.ui.specific.FileProvider;

/**
 * Implements a FileProvider for UI purposes. Note that it might not be very
 * reliable as long as we have not fixed login & multi repository issues that
 * will be addressed in the next version.
 * 
 * We are also very dependant of the repository architecture for file nodes. We
 * assume the content of the file is stored in a nt:resource child node of the
 * nt:file in the jcr:data property
 * 
 * @author bsinou
 * 
 */

public class JcrFileProvider implements FileProvider {

	private RepositoryNode repositoryNode;

	/**
	 * Must be set in order for the provider to be able to search the repository
	 * 
	 * @param repositoryNode
	 */
	public void setRepositoryNode(RepositoryNode repositoryNode) {
		this.repositoryNode = repositoryNode;
	}

	public byte[] getByteArrayFileFromId(String fileId) {
		InputStream fis = null;
		byte[] ba = null;
		Node child = getFileNodeFromId(fileId);
		try {
			fis = (InputStream) child.getProperty("jcr:data").getBinary()
					.getStream();
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
			fis = (InputStream) child.getProperty("jcr:data").getBinary()
					.getStream();
			return fis;
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot get stream from file node for Id "
					+ fileId, re);
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
		Object[] nodes = repositoryNode.getChildren();
		try {
			Node result = null;

			repos: for (int i = 0; i < nodes.length; i++) {
				WorkspaceNode wNode = (WorkspaceNode) nodes[i];
				result = wNode.getSession().getNodeByIdentifier(fileId);

				if (result == null)
					continue repos;

				// Ensure that the node have the correct type.
				if (!result.isNodeType("nt:file"))
					throw new ArgeoException(
							"Cannot open file children Node that are not of 'nt:resource' type.");

				Node child = result.getNodes().nextNode();
				if (child == null || !child.isNodeType("nt:resource"))
					throw new ArgeoException(
							"ERROR: IN the current implemented model, nt:file file node must have one and only one child of the nt:ressource, where actual data is stored");

				return child;
			}
		} catch (RepositoryException re) {
			throw new ArgeoException("Erreur while getting file node of ID "
					+ fileId, re);
		}

		throw new ArgeoException("File node not found for ID" + fileId);
	}
}
