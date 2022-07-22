package org.argeo.cms.ui.util;

import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;
import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_RESOURCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.swt.AbstractSwtImageManager;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/** Manages only public images so far. */
public class DefaultImageManager extends AbstractSwtImageManager<Node> {
	private final static CmsLog log = CmsLog.getLog(DefaultImageManager.class);

	/** @return null if not available */
	@Override
	public String getImageUrl(Node node) {
		return CmsUiUtils.getDataPathForUrl(node);
	}

	protected String getResourceName(Node node) {
		try {
			String workspace = node.getSession().getWorkspace().getName();
			if (node.hasNode(JCR_CONTENT))
				return workspace + '_' + node.getNode(JCR_CONTENT).getIdentifier();
			else
				return workspace + '_' + node.getIdentifier();
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	public Binary getImageBinary(Node node) {
		try {
			if (node.isNodeType(NT_FILE)) {
				return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();
			} else {
				return null;
			}
		} catch (RepositoryException e) {
			throw new JcrException(e);
		}
	}

	public Image getSwtImage(Node node) {
		InputStream inputStream = null;
		Binary binary = getImageBinary(node);
		if (binary == null)
			return null;
		try {
			inputStream = binary.getStream();
			return new Image(Display.getCurrent(), inputStream);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			JcrUtils.closeQuietly(binary);
		}
	}

	@Override
	public String uploadImage(Node context, Node parentNode, String fileName, InputStream in, String contentType) {
		InputStream inputStream = null;
		try {
			String previousResourceName = null;
			if (parentNode.hasNode(fileName)) {
				Node node = parentNode.getNode(fileName);
				previousResourceName = getResourceName(node);
				if (node.hasNode(JCR_CONTENT)) {
					node.getNode(JCR_CONTENT).remove();
					node.addNode(JCR_CONTENT, NT_RESOURCE);
				}
			}

			byte[] arr = IOUtils.toByteArray(in);
			Node fileNode = JcrUtils.copyBytesAsFile(parentNode, fileName, arr);
			inputStream = new ByteArrayInputStream(arr);
			ImageData id = new ImageData(inputStream);
			processNewImageFile(context, fileNode, id);

			String mime = contentType != null ? contentType : Files.probeContentType(Paths.get(fileName));
			if (mime != null) {
				fileNode.getNode(JCR_CONTENT).setProperty(Property.JCR_MIMETYPE, mime);
			}
			fileNode.getSession().save();

			// reset resource manager
			ResourceManager resourceManager = RWT.getResourceManager();
			if (previousResourceName != null && resourceManager.isRegistered(previousResourceName)) {
				resourceManager.unregister(previousResourceName);
				if (log.isDebugEnabled())
					log.debug("Unregistered image " + previousResourceName);
			}
			return CmsUiUtils.getDataPath(fileNode);
		} catch (IOException e) {
			throw new RuntimeException("Cannot upload image " + fileName + " in " + parentNode, e);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/** Does nothing by default. */
	protected void processNewImageFile(Node context, Node fileNode, ImageData id)
			throws RepositoryException, IOException {
	}

	@Override
	protected String noImg(Cms2DSize size) {
		return CmsUiUtils.noImg(size);
	}
}
