package org.argeo.cms.ui.util;

import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;
import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_RESOURCE;
import static org.argeo.cms.ui.CmsConstants.NO_IMAGE_SIZE;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/** Manages only public images so far. */
public class DefaultImageManager implements CmsImageManager {
	private final static Log log = LogFactory.getLog(DefaultImageManager.class);
//	private MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

	public Boolean load(Node node, Control control, Point preferredSize) throws RepositoryException {
		Point imageSize = getImageSize(node);
		Point size;
		String imgTag = null;
		if (preferredSize == null || imageSize.x == 0 || imageSize.y == 0
				|| (preferredSize.x == 0 && preferredSize.y == 0)) {
			if (imageSize.x != 0 && imageSize.y != 0) {
				// actual image size if completely known
				size = imageSize;
			} else {
				// no image if not completely known
				size = resizeTo(NO_IMAGE_SIZE, preferredSize != null ? preferredSize : imageSize);
				imgTag = CmsUiUtils.noImg(size);
			}

		} else if (preferredSize.x != 0 && preferredSize.y != 0) {
			// given size if completely provided
			size = preferredSize;
		} else {
			// at this stage :
			// image is completely known
			assert imageSize.x != 0 && imageSize.y != 0;
			// one and only one of the dimension as been specified
			assert preferredSize.x == 0 || preferredSize.y == 0;
			size = resizeTo(imageSize, preferredSize);
		}

		boolean loaded = false;
		if (control == null)
			return loaded;

		if (control instanceof Label) {
			if (imgTag == null) {
				// IMAGE RETRIEVED HERE
				imgTag = getImageTag(node, size);
				//
				if (imgTag == null)
					imgTag = CmsUiUtils.noImg(size);
				else
					loaded = true;
			}

			Label lbl = (Label) control;
			lbl.setText(imgTag);
			// lbl.setSize(size);
		} else if (control instanceof FileUpload) {
			FileUpload lbl = (FileUpload) control;
			lbl.setImage(CmsUiUtils.noImage(size));
			lbl.setSize(size);
			return loaded;
		} else
			loaded = false;

		return loaded;
	}

	private Point resizeTo(Point orig, Point constraints) {
		if (constraints.x != 0 && constraints.y != 0) {
			return constraints;
		} else if (constraints.x == 0 && constraints.y == 0) {
			return orig;
		} else if (constraints.y == 0) {// force width
			return new Point(constraints.x, scale(orig.y, orig.x, constraints.x));
		} else if (constraints.x == 0) {// force height
			return new Point(scale(orig.x, orig.y, constraints.y), constraints.y);
		}
		throw new CmsException("Cannot resize " + orig + " to " + constraints);
	}

	private int scale(int origDimension, int otherDimension, int otherConstraint) {
		return Math.round(origDimension * divide(otherConstraint, otherDimension));
	}

	private float divide(int a, int b) {
		return ((float) a) / ((float) b);
	}

	public Point getImageSize(Node node) throws RepositoryException {
		// TODO optimise
		Image image = getSwtImage(node);
		return new Point(image.getBounds().width, image.getBounds().height);
	}

	/** @return null if not available */
	@Override
	public String getImageTag(Node node) throws RepositoryException {
		return getImageTag(node, getImageSize(node));
	}

	private String getImageTag(Node node, Point size) throws RepositoryException {
		StringBuilder buf = getImageTagBuilder(node, size);
		if (buf == null)
			return null;
		return buf.append("/>").toString();
	}

	/** @return null if not available */
	@Override
	public StringBuilder getImageTagBuilder(Node node, Point size) throws RepositoryException {
		return getImageTagBuilder(node, Integer.toString(size.x), Integer.toString(size.y));
	}

	/** @return null if not available */
	private StringBuilder getImageTagBuilder(Node node, String width, String height) throws RepositoryException {
		String url = getImageUrl(node);
		if (url == null)
			return null;
		return CmsUiUtils.imgBuilder(url, width, height);
	}

	/** @return null if not available */
	@Override
	public String getImageUrl(Node node) throws RepositoryException {
		return CmsUiUtils.getDataPath(node);
	}

	protected String getResourceName(Node node) throws RepositoryException {
		String workspace = node.getSession().getWorkspace().getName();
		if (node.hasNode(JCR_CONTENT))
			return workspace + '_' + node.getNode(JCR_CONTENT).getIdentifier();
		else
			return workspace + '_' + node.getIdentifier();
	}

	public Binary getImageBinary(Node node) throws RepositoryException {
		if (node.isNodeType(NT_FILE)) {
			return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();
		} else {
			return null;
		}
	}

	public Image getSwtImage(Node node) throws RepositoryException {
		InputStream inputStream = null;
		Binary binary = getImageBinary(node);
		if (binary == null)
			return null;
		try {
			inputStream = binary.getStream();
			return new Image(Display.getCurrent(), inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
			JcrUtils.closeQuietly(binary);
		}
	}

	@Override
	public String uploadImage(Node context, Node parentNode, String fileName, InputStream in, String contentType)
			throws RepositoryException {
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
			throw new CmsException("Cannot upload image " + fileName + " in " + parentNode, e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/** Does nothing by default. */
	protected void processNewImageFile(Node context, Node fileNode, ImageData id)
			throws RepositoryException, IOException {
	}
}
