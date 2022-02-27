package org.argeo.cms.ui.widgets;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.api.cms.Cms2DSize;
import org.argeo.api.cms.CmsImageManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.internal.JcrFileUploadReceiver;
import org.argeo.cms.ui.viewers.NodePart;
import org.argeo.cms.ui.viewers.Section;
import org.argeo.cms.ui.viewers.SectionPart;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadListener;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** An image within the Argeo Text framework */
public class Img extends EditableImage implements SectionPart, NodePart {
	private static final long serialVersionUID = 6233572783968188476L;

	private final Section section;

	private final CmsImageManager<Control, Node> imageManager;
	private FileUploadHandler currentUploadHandler = null;
	private FileUploadListener fileUploadListener;

	public Img(Composite parent, int swtStyle, Node imgNode, Cms2DSize preferredImageSize) throws RepositoryException {
		this(Section.findSection(parent), parent, swtStyle, imgNode, preferredImageSize, null);
		setStyle(TextStyles.TEXT_IMAGE);
	}

	public Img(Composite parent, int swtStyle, Node imgNode) throws RepositoryException {
		this(Section.findSection(parent), parent, swtStyle, imgNode, null, null);
		setStyle(TextStyles.TEXT_IMAGE);
	}

	public Img(Composite parent, int swtStyle, Node imgNode, CmsImageManager<Control, Node> imageManager)
			throws RepositoryException {
		this(Section.findSection(parent), parent, swtStyle, imgNode, null, imageManager);
		setStyle(TextStyles.TEXT_IMAGE);
	}

	Img(Section section, Composite parent, int swtStyle, Node imgNode, Cms2DSize preferredImageSize,
			CmsImageManager<Control, Node> imageManager) throws RepositoryException {
		super(parent, swtStyle, imgNode, false, preferredImageSize);
		this.section = section;
		this.imageManager = imageManager != null ? imageManager
				: (CmsImageManager<Control, Node>) CmsSwtUtils.getCmsView(section).getImageManager();
		CmsSwtUtils.style(this, TextStyles.TEXT_IMG);
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing()) {
			try {
				return createImageChooser(box, style);
			} catch (RepositoryException e) {
				throw new JcrException("Cannot create image chooser", e);
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
		Node imgNode = getNode();
		boolean loaded = imageManager.load(imgNode, lbl, getPreferredImageSize());
		// getParent().layout();
		return loaded;
	}

	protected Node getUploadFolder() {
		return Jcr.getParent(getNode());
	}

	protected String getUploadName() {
		Node node = getNode();
		return Jcr.getName(node) + '[' + Jcr.getIndex(node) + ']';
	}

	protected CmsImageManager<Control, Node> getImageManager() {
		return imageManager;
	}

	protected Control createImageChooser(Composite box, String style) throws RepositoryException {
		JcrFileUploadReceiver receiver = new JcrFileUploadReceiver(this, getUploadFolder(), getUploadName(),
				imageManager);
		if (currentUploadHandler != null)
			currentUploadHandler.dispose();
		currentUploadHandler = prepareUpload(receiver);
		final ServerPushSession pushSession = new ServerPushSession();
		final FileUpload fileUpload = new FileUpload(box, SWT.NONE);
		CmsSwtUtils.style(fileUpload, style);
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
