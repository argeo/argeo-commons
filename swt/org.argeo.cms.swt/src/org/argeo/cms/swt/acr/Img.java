package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.widgets.EditableImage;
import org.argeo.cms.ux.acr.ContentPart;
import org.argeo.eclipse.ui.specific.CmsFileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** An image within the Argeo Text framework */
public class Img extends EditableImage implements SwtSectionPart, ContentPart {
	private static final long serialVersionUID = 6233572783968188476L;

	private final SwtSection section;

	private final CmsImageManager<Control, Content> imageManager;
//	private FileUploadHandler currentUploadHandler = null;
//	private FileUploadListener fileUploadListener;

	public Img(Composite parent, int swtStyle, Content imgNode, Cms2DSize preferredImageSize) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, preferredImageSize, null);
//		setStyle(TextStyles.TEXT_IMAGE);
	}

	public Img(Composite parent, int swtStyle, Content imgNode) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, null, null);
//		setStyle(TextStyles.TEXT_IMAGE);
	}

	public Img(Composite parent, int swtStyle, Content imgNode, CmsImageManager<Control, Content> imageManager) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, null, imageManager);
//		setStyle(TextStyles.TEXT_IMAGE);
	}

	Img(SwtSection section, Composite parent, int swtStyle, Content imgNode, Cms2DSize preferredImageSize,
			CmsImageManager<Control, Content> imageManager) {
		super(parent, swtStyle, preferredImageSize);
		this.section = section;
		this.imageManager = imageManager != null ? imageManager
				: (CmsImageManager<Control, Content>) CmsSwtUtils.getCmsView(section).getImageManager();
//		CmsSwtUtils.style(this, TextStyles.TEXT_IMG);
		setData(imgNode);
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing()) {
			return createImageChooser(box, style);
		} else {
			return createLabel(box, style);
		}
	}

	@Override
	public synchronized void stopEditing() {
		super.stopEditing();
//		fileUploadListener = null;
	}

	@Override
	protected synchronized Boolean load(Control lbl) {
		Content imgNode = getContent();
		boolean loaded = imageManager.load(imgNode, lbl, getPreferredImageSize());
		// getParent().layout();
		return loaded;
	}

	protected Content getUploadFolder() {
		return getContent().getParent();
	}

	protected String getUploadName() {
		Content node = getContent();
		// TODO centralise pattern?
		return NamespaceUtils.toPrefixedName(node.getName()) + '[' + node.getSiblingIndex() + ']';
	}

	protected CmsImageManager<Control, Content> getImageManager() {
		return imageManager;
	}

	protected Control createImageChooser(Composite box, String style) {
//		JcrFileUploadReceiver receiver = new JcrFileUploadReceiver(this, getUploadFolder(), getUploadName(),
//				imageManager);
//		if (currentUploadHandler != null)
//			currentUploadHandler.dispose();
//		currentUploadHandler = prepareUpload(receiver);
//		final ServerPushSession pushSession = new ServerPushSession();
		final CmsFileUpload fileUpload = new CmsFileUpload(box, SWT.NONE);
		CmsSwtUtils.style(fileUpload, style);
		fileUpload.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = -9158471843941668562L;

			@Override
			public void widgetSelected(SelectionEvent e) {
//				pushSession.start();
//				fileUpload.submit(currentUploadHandler.getUploadUrl());
			}
		});
		return fileUpload;
	}

//	protected FileUploadHandler prepareUpload(FileUploadReceiver receiver) {
//		final FileUploadHandler uploadHandler = new FileUploadHandler(receiver);
//		if (fileUploadListener != null)
//			uploadHandler.addUploadListener(fileUploadListener);
//		return uploadHandler;
//	}

	@Override
	public SwtSection getSection() {
		return section;
	}

//	public void setFileUploadListener(FileUploadListener fileUploadListener) {
//		this.fileUploadListener = fileUploadListener;
//		if (currentUploadHandler != null)
//			currentUploadHandler.addUploadListener(fileUploadListener);
//	}

	@Override
	public Content getContent() {
		return (Content) getData();
	}

	@Override
	public String getPartId() {
		return ((ProvidedContent) getContent()).getSessionLocalId();
	}

	@Override
	public String toString() {
		return "Img #" + getPartId();
	}

}
