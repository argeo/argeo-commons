package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.acr.ContentPart;
import org.argeo.eclipse.ui.specific.CmsFileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** An image within the Argeo Text framework */
public class Img extends LinkedControl implements SwtSectionPart, ContentPart {
	private static final long serialVersionUID = 6233572783968188476L;

	private final SwtSection section;

	private final CmsImageManager<Control, Content> imageManager;

	private Cms2DSize preferredImageSize;

	public Img(Composite parent, int swtStyle, Content imgNode, Cms2DSize preferredImageSize) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, preferredImageSize, null);
	}

	public Img(Composite parent, int swtStyle, Content imgNode) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, null, null);
	}

	public Img(Composite parent, int swtStyle, Content imgNode, CmsImageManager<Control, Content> imageManager) {
		this(SwtSection.findSection(parent), parent, swtStyle, imgNode, null, imageManager);
	}

	Img(SwtSection section, Composite parent, int swtStyle, Content imgNode, Cms2DSize preferredImageSize,
			CmsImageManager<Control, Content> imageManager) {
		super(parent, swtStyle);
		this.preferredImageSize = preferredImageSize;
		this.section = section;
		this.imageManager = imageManager != null ? imageManager : CmsSwtUtils.getCmsView(section).getImageManager();
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

	protected synchronized Boolean load(Control lbl) {
		Content imgNode = getContent();
		boolean loaded = imageManager.load(imgNode, lbl, preferredImageSize, toUri());
		return loaded;
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle());
		// lbl.setLayoutData(CmsUiUtils.fillWidth());
		CmsSwtUtils.markup(lbl);
		CmsSwtUtils.style(lbl, style);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		load(lbl);
		return lbl;
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

	public void setPreferredSize(Cms2DSize size) {
		this.preferredImageSize = size;
	}

	public Cms2DSize getPreferredImageSize() {
		return preferredImageSize;
	}

	public void setPreferredImageSize(Cms2DSize preferredImageSize) {
		this.preferredImageSize = preferredImageSize;
	}
}
