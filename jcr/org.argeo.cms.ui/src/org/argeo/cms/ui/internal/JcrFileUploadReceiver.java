package org.argeo.cms.ui.internal;

import static javax.jcr.nodetype.NodeType.NT_FILE;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FilenameUtils;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.cms.ui.widgets.Img;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadReceiver;

public class JcrFileUploadReceiver extends FileUploadReceiver {
	private Img img;
	private final Node parentNode;
	private final String nodeName;
	private final CmsImageManager imageManager;

	/** If nodeName is null, use the uploaded file name */
	public JcrFileUploadReceiver(Img img, Node parentNode, String nodeName, CmsImageManager imageManager) {
		super();
		this.img = img;
		this.parentNode = parentNode;
		this.nodeName = nodeName;
		this.imageManager = imageManager;
	}

	@Override
	public void receive(InputStream stream, FileDetails details) throws IOException {
		try {
			String fileName = nodeName != null ? nodeName : details.getFileName();
			String contentType = details.getContentType();
			if (isImage(details.getFileName(), contentType)) {
				imageManager.uploadImage(img.getNode(),parentNode, fileName, stream, contentType);
				return;
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
			throw new JcrException("Cannot receive " + details, e);
		}
	}

	protected Boolean isImage(String fileName, String contentType) {
		String ext = FilenameUtils.getExtension(fileName);
		return ext != null && (ext.equals("png") || ext.equalsIgnoreCase("jpg"));
	}

	protected void processNewFile(Node node) {

	}

}
