package org.argeo.cms.internal;

import static javax.jcr.nodetype.NodeType.NT_FILE;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FilenameUtils;
import org.argeo.cms.CmsException;
import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsNames;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.addons.fileupload.FileDetails;
import org.eclipse.rap.addons.fileupload.FileUploadReceiver;

public class JcrFileUploadReceiver extends FileUploadReceiver implements
		CmsNames {
	private final Node parentNode;
	private final String nodeName;
	private final CmsImageManager imageManager;

	/** If nodeName is null, use the uploaded file name */
	public JcrFileUploadReceiver(Node parentNode, String nodeName,
			CmsImageManager imageManager) {
		super();
		this.parentNode = parentNode;
		this.nodeName = nodeName;
		this.imageManager = imageManager;
	}

	@Override
	public void receive(InputStream stream, FileDetails details)
			throws IOException {
		try {
			String fileName = nodeName != null ? nodeName : details
					.getFileName();
			String contentType = details.getContentType();
			if (isImage(details.getFileName(), contentType)) {
				imageManager.uploadImage(parentNode, fileName, stream);
				return;
				// InputStream inputStream = new ByteArrayInputStream(arr);
				// ImageData id = new ImageData(inputStream);
				// fileNode.addMixin(CmsTypes.CMS_IMAGE);
				// fileNode.setProperty(CMS_IMAGE_WIDTH, id.width);
				// fileNode.setProperty(CMS_IMAGE_HEIGHT, id.height);
			}

			Node fileNode;
			if (parentNode.hasNode(fileName)) {
				fileNode = parentNode.getNode(fileName);
				if (!fileNode.isNodeType(NT_FILE))
					fileNode.remove();
			}
			fileNode = JcrUtils.copyStreamAsFile(parentNode, fileName, stream);

			if (contentType != null) {
				fileNode.addMixin(NodeType.MIX_MIMETYPE);
				fileNode.setProperty(Property.JCR_MIMETYPE, contentType);
			}
			processNewFile(fileNode);
			fileNode.getSession().save();
		} catch (RepositoryException e) {
			throw new CmsException("cannot receive " + details, e);
		}
	}

	protected Boolean isImage(String fileName, String contentType) {
		String ext = FilenameUtils.getExtension(fileName);
		return ext != null
				&& (ext.equals("png") || ext.equalsIgnoreCase("jpg"));
	}

	protected void processNewFile(Node node) {

	}

}
