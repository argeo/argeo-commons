package org.argeo.cms.ui.internal;

import javax.jcr.RepositoryException;

import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.cms.ui.widgets.EditableImage;
import org.argeo.cms.ux.AbstractImageManager;
import org.argeo.cms.ux.CmsUxUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/** NOT working yet. */
public class SimpleEditableImage extends EditableImage {
	private static final long serialVersionUID = -5689145523114022890L;

	private String src;
	private Cms2DSize imageSize;

	public SimpleEditableImage(Composite parent, int swtStyle) {
		super(parent, swtStyle);
		// load(getControl());
		getParent().layout();
	}

	public SimpleEditableImage(Composite parent, int swtStyle, String src, Cms2DSize imageSize) {
		super(parent, swtStyle);
		this.src = src;
		this.imageSize = imageSize;
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing()) {
			return createText(box, style);
		} else {
			return createLabel(box, style);
		}
	}

	protected String createImgTag() throws RepositoryException {
		String imgTag;
		if (src != null)
			imgTag = CmsUxUtils.img(src, imageSize);
		else
			imgTag = CmsUiUtils.noImg(imageSize != null ? imageSize : AbstractImageManager.NO_IMAGE_SIZE);
		return imgTag;
	}

	protected Text createText(Composite box, String style) {
		Text text = new Text(box, getStyle());
		CmsSwtUtils.style(text, style);
		return text;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public Cms2DSize getImageSize() {
		return imageSize;
	}

	public void setImageSize(Cms2DSize imageSize) {
		this.imageSize = imageSize;
	}

}
