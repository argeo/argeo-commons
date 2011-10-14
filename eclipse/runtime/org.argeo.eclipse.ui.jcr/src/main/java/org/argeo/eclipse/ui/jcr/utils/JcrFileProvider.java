package org.argeo.eclipse.ui.jcr.utils;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.specific.FileProvider;

/**
 * Implements a FileProvider for UI purposes. Note that it might not be very
 * reliable as long as we have not fixed login & multi repository issues that
 * will be addressed in the next version.
 * 
 * NOTE: id used here is the real id of the JCR Node, not the JCR Path
 * 
 * Relies on common approach for JCR file handling implementation.
 * 
 */

public class JcrFileProvider implements FileProvider {

	// private Object[] rootNodes;
	private Node refNode;

	/**
	 * Must be set in order for the provider to be able to get current session
	 * and thus have the ability to get the file node corresponding to a given
	 * file ID
	 * 
	 * FIXME : this introduces some concurrences ISSUES.
	 * 
	 * @param repositoryNode
	 */
	public void setReferenceNode(Node refNode) {
		this.refNode = refNode;
	}

	/**
	 * Must be set in order for the provider to be able to search the repository
	 * Provided object might be either JCR Nodes or UI RepositoryNode for the
	 * time being.
	 * 
	 * @param repositoryNode
	 */
	// public void setRootNodes(Object[] rootNodes) {
	// List<Object> tmpNodes = new ArrayList<Object>();
	// for (int i = 0; i < rootNodes.length; i++) {
	// Object obj = rootNodes[i];
	// if (obj instanceof Node) {
	// tmpNodes.add(obj);
	// } else if (obj instanceof RepositoryRegister) {
	// RepositoryRegister repositoryRegister = (RepositoryRegister) obj;
	// Map<String, Repository> repositories = repositoryRegister
	// .getRepositories();
	// for (String name : repositories.keySet()) {
	// // tmpNodes.add(new RepositoryNode(name, repositories
	// // .get(name)));
	// }
	//
	// }
	// }
	// this.rootNodes = tmpNodes.toArray();
	// }

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
				throw new ArgeoException("File node not found for ID" + fileId);

			// Ensure that the node have the correct type.
			if (!result.isNodeType(NodeType.NT_FILE))
				throw new ArgeoException(
						"Cannot open file children Node that are not of '"
								+ NodeType.NT_RESOURCE + "' type.");

			// Get the usefull part of the Node
			Node child = result.getNodes().nextNode();
			if (child == null || !child.isNodeType(NodeType.NT_RESOURCE))
				throw new ArgeoException(
						"ERROR: IN the current implemented model, '"
								+ NodeType.NT_FILE
								+ "' file node must have one and only one child of the nt:ressource, where actual data is stored");
			return child;

		} catch (RepositoryException re) {
			throw new ArgeoException("Erreur while getting file node of ID "
					+ fileId, re);
		}
	}
}
