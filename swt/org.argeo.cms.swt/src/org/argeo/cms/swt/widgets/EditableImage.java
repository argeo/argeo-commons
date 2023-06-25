package org.argeo.cms.swt.widgets;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.AbstractImageManager;
import org.argeo.cms.ux.CmsUxUtils;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** A stylable and editable image. */
@Deprecated
public abstract class EditableImage extends StyledControl {
	private static final long serialVersionUID = -5689145523114022890L;
	private final static CmsLog log = CmsLog.getLog(EditableImage.class);

	private Cms2DSize preferredImageSize;
	private Boolean loaded = false;

	public EditableImage(Composite parent, int swtStyle) {
		super(parent, swtStyle);
	}

	public EditableImage(Composite parent, int swtStyle, Cms2DSize preferredImageSize) {
		super(parent, swtStyle);
		this.preferredImageSize = preferredImageSize;
	}

	@Override
	protected void setContainerLayoutData(Composite composite) {
		// composite.setLayoutData(fillWidth());
	}

	@Override
	protected void setControlLayoutData(Control control) {
		// control.setLayoutData(fillWidth());
	}

	/** To be overriden. */
	protected String createImgTag() {
		return noImg(preferredImageSize != null ? preferredImageSize : new Cms2DSize(getSize().x, getSize().y));
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

	/** To be overriden. */
	protected synchronized Boolean load(Control control) {
		String imgTag;
		try {
			imgTag = createImgTag();
		} catch (Exception e) {
			// throw new CmsException("Cannot retrieve image", e);
			log.error("Cannot retrieve image", e);
			imgTag = noImg(preferredImageSize);
			loaded = false;
		}

		if (imgTag == null) {
			loaded = false;
			imgTag = noImg(preferredImageSize);
		} else
			loaded = true;
		if (control != null) {
			((Label) control).setText(imgTag);
			control.setSize(
					preferredImageSize != null ? new Point(preferredImageSize.width(), preferredImageSize.height())
							: getSize());
		} else {
			loaded = false;
		}
		getParent().layout();
		return loaded;
	}

	public void setPreferredSize(Cms2DSize size) {
		this.preferredImageSize = size;
		if (!loaded) {
			load((Label) getControl());
		}
	}

	protected Text createText(Composite box, String style) {
		Text text = new Text(box, getStyle());
		CmsSwtUtils.style(text, style);
		return text;
	}

	public Cms2DSize getPreferredImageSize() {
		return preferredImageSize;
	}

	public static String noImg(Cms2DSize size) {
//		ResourceManager rm = RWT.getResourceManager();
//		String noImgPath=rm.getLocation(AbstractImageManager.NO_IMAGE);
		// FIXME load it via package service
		String noImgPath = "";
		return CmsUxUtils.img(noImgPath, size);
	}

	public static String noImg() {
		return noImg(AbstractImageManager.NO_IMAGE_SIZE);
	}

}
