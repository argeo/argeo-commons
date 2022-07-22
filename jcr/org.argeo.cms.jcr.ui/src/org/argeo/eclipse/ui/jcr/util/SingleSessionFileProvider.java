package org.argeo.eclipse.ui.jcr.util;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.FileProvider;

/**
 * Implements a FileProvider for UI purposes. Unlike the
 * <code> JcrFileProvider </code>, it relies on a single session and manages
 * nodes with path only.
 * 
 * Note that considered id is the JCR path
 * 
 * Relies on common approach for JCR file handling implementation.
 */
@SuppressWarnings("deprecation")
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
			fis = (InputStream) child.getProperty(Property.JCR_DATA)
					.getBinary().getStream();
			return fis;
		} catch (RepositoryException re) {
			throw new EclipseUiException("Cannot get stream from file node for Id "
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
				throw new EclipseUiException("File node not found for ID" + fileId);

			// Ensure that the node have the correct type.
			if (!result.isNodeType(NodeType.NT_FILE))
				throw new EclipseUiException(
						"Cannot open file children Node that are not of "
								+ NodeType.NT_RESOURCE + " type.");

			Node child = result.getNodes().nextNode();
			if (child == null || !child.isNodeType(NodeType.NT_RESOURCE))
				throw new EclipseUiException(
						"ERROR: IN the current implemented model, "
								+ NodeType.NT_FILE
								+ "  file node must have one and only one child of the nt:ressource, where actual data is stored");
			return child;
		} catch (RepositoryException re) {
			throw new EclipseUiException("Erreur while getting file node of ID "
					+ fileId, re);
		}
	}
}
