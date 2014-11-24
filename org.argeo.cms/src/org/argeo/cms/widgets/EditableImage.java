package org.argeo.cms.widgets;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsUtils;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** A stylable and editable image. */
public abstract class EditableImage extends StyledControl {
	private static final long serialVersionUID = -5689145523114022890L;
	private final static Log log = LogFactory.getLog(EditableImage.class);

	private Point preferredImageSize;
	private Boolean loaded = false;

	public EditableImage(Composite parent, int swtStyle) {
		super(parent, swtStyle);
	}

	public EditableImage(Composite parent, int swtStyle,
			Point preferredImageSize) {
		super(parent, swtStyle);
		this.preferredImageSize = preferredImageSize;
	}

	public EditableImage(Composite parent, int style, Node node,
			boolean cacheImmediately, Point preferredImageSize)
			throws RepositoryException {
		super(parent, style, node, cacheImmediately);
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
	protected String createImgTag() throws RepositoryException {
		return CmsUtils.noImg(preferredImageSize != null ? preferredImageSize
				: getSize());
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle());
		// lbl.setLayoutData(CmsUtils.fillWidth());
		CmsUtils.markup(lbl);
		CmsUtils.style(lbl, style);
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
			imgTag = CmsUtils.noImg(preferredImageSize);
			loaded = false;
		}

		if (imgTag == null) {
			loaded = false;
			imgTag = CmsUtils.noImg(preferredImageSize);
		} else
			loaded = true;
		if (control != null) {
			((Label) control).setText(imgTag);
			control.setSize(preferredImageSize != null ? preferredImageSize
					: getSize());
		} else {
			loaded = false;
		}
		getParent().layout();
		return loaded;
	}

	public void setPreferredSize(Point size) {
		this.preferredImageSize = size;
		if (!loaded) {
			load((Label) getControl());
		}
	}

	protected Text createText(Composite box, String style) {
		Text text = new Text(box, getStyle());
		CmsUtils.style(text, style);
		return text;
	}

	public Point getPreferredImageSize() {
		return preferredImageSize;
	}

}
