package org.argeo.cms.ui.util;

import static javax.jcr.Node.JCR_CONTENT;
import static javax.jcr.Property.JCR_DATA;
import static javax.jcr.nodetype.NodeType.NT_FILE;
import static javax.jcr.nodetype.NodeType.NT_RESOURCE;
import static org.argeo.cms.ui.CmsUiConstants.NO_IMAGE_SIZE;

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
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.jcr.JcrException;
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
public class DefaultImageManager implements CmsImageManager<Control, Node> {
	private final static CmsLog log = CmsLog.getLog(DefaultImageManager.class);
//	private MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

	public Boolean load(Node node, Control control, Cms2DSize preferredSize) {
		Cms2DSize imageSize = getImageSize(node);
		Cms2DSize size;
		String imgTag = null;
		if (preferredSize == null || imageSize.getWidth() == 0 || imageSize.getHeight() == 0
				|| (preferredSize.getWidth() == 0 && preferredSize.getHeight() == 0)) {
			if (imageSize.getWidth() != 0 && imageSize.getHeight() != 0) {
				// actual image size if completely known
				size = imageSize;
			} else {
				// no image if not completely known
				size = resizeTo(NO_IMAGE_SIZE, preferredSize != null ? preferredSize : imageSize);
				imgTag = CmsUiUtils.noImg(size);
			}

		} else if (preferredSize.getWidth() != 0 && preferredSize.getHeight() != 0) {
			// given size if completely provided
			size = preferredSize;
		} else {
			// at this stage :
			// image is completely known
			assert imageSize.getWidth() != 0 && imageSize.getHeight() != 0;
			// one and only one of the dimension as been specified
			assert preferredSize.getWidth() == 0 || preferredSize.getHeight() == 0;
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
			lbl.setSize(new Point(size.getWidth(), size.getHeight()));
			return loaded;
		} else
			loaded = false;

		return loaded;
	}

	private Cms2DSize resizeTo(Cms2DSize orig, Cms2DSize constraints) {
		if (constraints.getWidth() != 0 && constraints.getHeight() != 0) {
			return constraints;
		} else if (constraints.getWidth() == 0 && constraints.getHeight() == 0) {
			return orig;
		} else if (constraints.getHeight() == 0) {// force width
			return new Cms2DSize(constraints.getWidth(),
					scale(orig.getHeight(), orig.getWidth(), constraints.getWidth()));
		} else if (constraints.getWidth() == 0) {// force height
			return new Cms2DSize(scale(orig.getWidth(), orig.getHeight(), constraints.getHeight()),
					constraints.getHeight());
		}
		throw new IllegalArgumentException("Cannot resize " + orig + " to " + constraints);
	}

	private int scale(int origDimension, int otherDimension, int otherConstraint) {
		return Math.round(origDimension * divide(otherConstraint, otherDimension));
	}

	private float divide(int a, int b) {
		return ((float) a) / ((float) b);
	}

	public Cms2DSize getImageSize(Node node) {
		// TODO optimise
		Image image = getSwtImage(node);
		return new Cms2DSize(image.getBounds().width, image.getBounds().height);
	}

	/** @return null if not available */
	@Override
	public String getImageTag(Node node) {
		return getImageTag(node, getImageSize(node));
	}

	private String getImageTag(Node node, Cms2DSize size) {
		StringBuilder buf = getImageTagBuilder(node, size);
		if (buf == null)
			return null;
		return buf.append("/>").toString();
	}

	/** @return null if not available */
	@Override
	public StringBuilder getImageTagBuilder(Node node, Cms2DSize size) {
		return getImageTagBuilder(node, Integer.toString(size.getWidth()), Integer.toString(size.getHeight()));
	}

	/** @return null if not available */
	private StringBuilder getImageTagBuilder(Node node, String width, String height) {
		String url = getImageUrl(node);
		if (url == null)
			return null;
		return CmsUiUtils.imgBuilder(url, width, height);
	}

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
}
