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
		try {
			Object[] nodes = repositoryNode.getChildren();

			repos: for (int i = 0; i < nodes.length; i++) {
				WorkspaceNode wNode = (WorkspaceNode) nodes[i];
				Node node = null;
				node = wNode.getSession().getNodeByIdentifier(fileId);

				if (node == null)
					continue repos;

				if (!node.isNodeType("nt:file"))
					throw new ArgeoException(
							"Cannot open file children Node that are not of 'nt:resource' type.");

				Node child = node.getNodes().nextNode();
				if (!child.isNodeType("nt:resource"))
					throw new ArgeoException(
							"Cannot open file children Node that are not of 'nt:resource' type.");

				InputStream fis = null;
				byte[] ba = null;
				try {
					fis = (InputStream) child.getProperty("jcr:data")
							.getBinary().getStream();
					ba = IOUtils.toByteArray(fis);

				} catch (Exception e) {
					throw new ArgeoException("Stream error while opening file",
							e);
				} finally {
					IOUtils.closeQuietly(fis);
				}
				if (ba != null)
					return ba;
			}

		} catch (RepositoryException re) {
			throw new ArgeoException("RepositoryException while reading file ",
					re);
		}

		throw new ArgeoException("File not found for ID " + fileId);
	}
}
