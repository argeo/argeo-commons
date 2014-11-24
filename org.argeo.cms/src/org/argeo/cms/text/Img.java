package org.argeo.cms.text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsImageManager;
import org.argeo.cms.CmsSession;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.internal.JcrFileUploadReceiver;
import org.argeo.cms.viewers.NodePart;
import org.argeo.cms.viewers.Section;
import org.argeo.cms.viewers.SectionPart;
import org.argeo.cms.widgets.EditableImage;
import org.eclipse.rap.addons.fileupload.FileUploadHandler;
import org.eclipse.rap.addons.fileupload.FileUploadListener;
import org.eclipse.rap.addons.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** An image within the Argeo Text framework */
public class Img extends EditableImage implements SectionPart, NodePart {
	private static final long serialVersionUID = 6233572783968188476L;

	private final Section section;

	private final CmsImageManager imageManager;
	private FileUploadHandler currentUploadHandler = null;
	private FileUploadListener fileUploadListener;

	public Img(Composite parent, int swtStyle, Node imgNode,
			Point preferredImageSize) throws RepositoryException {
		this(Section.findSection(parent), parent, swtStyle, imgNode,
				preferredImageSize);
		setStyle(TextStyles.TEXT_IMAGE);
	}

	public Img(Composite parent, int swtStyle, Node imgNode)
			throws RepositoryException {
		this(Section.findSection(parent), parent, swtStyle, imgNode, null);
		setStyle(TextStyles.TEXT_IMAGE);
	}

	Img(Section section, Composite parent, int swtStyle, Node imgNode,
			Point preferredImageSize) throws RepositoryException {
		super(parent, swtStyle, imgNode, false, preferredImageSize);
		this.section = section;
		imageManager = CmsSession.current.get().getImageManager();
		CmsUtils.style(this, TextStyles.TEXT_IMG);
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing()) {
			try {
				return createImageChooser(box, style);
			} catch (RepositoryException e) {
				throw new CmsException("Cannot create image chooser", e);
			}
		} else {
			return createLabel(box, style);
		}
	}

	@Override
	public synchronized void stopEditing() {
		super.stopEditing();
		fileUploadListener = null;
	}

	@Override
	protected synchronized Boolean load(Control lbl) {
		try {
			Node imgNode = getNode();
			boolean loaded = imageManager.load(imgNode, lbl,
					getPreferredImageSize());
			// getParent().layout();
			return loaded;
		} catch (RepositoryException e) {
			throw new CmsException("Cannot load " + getNodeId()
					+ " from image manager", e);
		}
	}

	protected Control createImageChooser(Composite box, String style)
			throws RepositoryException {
		// FileDialog fileDialog = new FileDialog(getShell());
		// fileDialog.open();
		// String fileName = fileDialog.getFileName();
		CmsImageManager imageManager = CmsSession.current.get()
				.getImageManager();
		Node node = getNode();
		JcrFileUploadReceiver receiver = new JcrFileUploadReceiver(
				node.getParent(), node.getName() + '[' + node.getIndex() + ']',
				imageManager);
		if (currentUploadHandler != null)
			currentUploadHandler.dispose();
		currentUploadHandler = prepareUpload(receiver);
		final ServerPushSession pushSession = new ServerPushSession();
		final FileUpload fileUpload = new FileUpload(box, SWT.NONE);
		CmsUtils.style(fileUpload, style);
		fileUpload.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = -9158471843941668562L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				pushSession.start();
				fileUpload.submit(currentUploadHandler.getUploadUrl());
			}
		});
		return fileUpload;
	}

	protected FileUploadHandler prepareUpload(FileUploadReceiver receiver) {
		final FileUploadHandler uploadHandler = new FileUploadHandler(receiver);
		if (fileUploadListener != null)
			uploadHandler.addUploadListener(fileUploadListener);
		return uploadHandler;
	}

	@Override
	public Section getSection() {
		return section;
	}

	public void setFileUploadListener(FileUploadListener fileUploadListener) {
		this.fileUploadListener = fileUploadListener;
		if (currentUploadHandler != null)
			currentUploadHandler.addUploadListener(fileUploadListener);
	}

	@Override
	public Node getItem() throws RepositoryException {
		return getNode();
	}

	@Override
	public String getPartId() {
		return getNodeId();
	}

	@Override
	public String toString() {
		return "Img #" + getPartId();
	}

}
