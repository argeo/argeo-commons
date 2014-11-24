package org.argeo.cms.internal;

import javax.jcr.RepositoryException;

import org.argeo.cms.CmsUtils;
import org.argeo.cms.widgets.EditableImage;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/** NOT working yet. */
public class SimpleEditableImage extends EditableImage {
	private static final long serialVersionUID = -5689145523114022890L;

	private String src;
	private Point imageSize;

	public SimpleEditableImage(Composite parent, int swtStyle) {
		super(parent, swtStyle);
		// load(getControl());
		getParent().layout();
	}

	public SimpleEditableImage(Composite parent, int swtStyle, String src,
			Point imageSize) {
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
			imgTag = CmsUtils.img(src, imageSize);
		else
			imgTag = CmsUtils.noImg(imageSize != null ? imageSize
					: NO_IMAGE_SIZE);
		return imgTag;
	}

	protected Text createText(Composite box, String style) {
		Text text = new Text(box, getStyle());
		CmsUtils.style(text, style);
		return text;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public Point getImageSize() {
		return imageSize;
	}

	public void setImageSize(Point imageSize) {
		this.imageSize = imageSize;
	}

}
