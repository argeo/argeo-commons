package org.argeo.cms.ui.widgets;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.api.cms.Cms2DSize;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** A stylable and editable image. */
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

	public EditableImage(Composite parent, int style, Node node, boolean cacheImmediately, Cms2DSize preferredImageSize)
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
		return CmsUiUtils
				.noImg(preferredImageSize != null ? preferredImageSize : new Cms2DSize(getSize().x, getSize().y));
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
			imgTag = CmsUiUtils.noImg(preferredImageSize);
			loaded = false;
		}

		if (imgTag == null) {
			loaded = false;
			imgTag = CmsUiUtils.noImg(preferredImageSize);
		} else
			loaded = true;
		if (control != null) {
			((Label) control).setText(imgTag);
			control.setSize(preferredImageSize != null
					? new Point(preferredImageSize.getWidth(), preferredImageSize.getHeight())
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

}
