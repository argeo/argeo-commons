package org.argeo.cms.swt;

import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.ux.AbstractImageManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** Manages only public images so far. */
public abstract class AbstractSwtImageManager<M> extends AbstractImageManager<Control, M> {
	protected abstract Image getSwtImage(M node);

	protected abstract String noImg(Cms2DSize size);

	public Boolean load(M node, Control control, Cms2DSize preferredSize) {
		Cms2DSize imageSize = getImageSize(node);
		Cms2DSize size;
		String imgTag = null;
		if (preferredSize == null || imageSize.getWidth() == 0 || imageSize.getHeight() == 0
				|| (preferredSize.getWidth() == 0 && preferredSize.getHeight() == 0)) {
			if (imageSize.getWidth() != 0 && imageSize.getHeight() != 0) {
				// actual image size if completely known
				size = imageSize;
			} else {
				// no image if not completely known
				size = resizeTo(NO_IMAGE_SIZE, preferredSize != null ? preferredSize : imageSize);
				imgTag = noImg(size);
			}

		} else if (preferredSize.getWidth() != 0 && preferredSize.getHeight() != 0) {
			// given size if completely provided
			size = preferredSize;
		} else {
			// at this stage :
			// image is completely known
			assert imageSize.getWidth() != 0 && imageSize.getHeight() != 0;
			// one and only one of the dimension as been specified
			assert preferredSize.getWidth() == 0 || preferredSize.getHeight() == 0;
			size = resizeTo(imageSize, preferredSize);
		}

		boolean loaded = false;
		if (control == null)
			return loaded;

		if (control instanceof Label) {
			if (imgTag == null) {
				// IMAGE RETRIEVED HERE
				imgTag = getImageTag(node, size);
				//
				if (imgTag == null)
					imgTag = noImg(size);
				else
					loaded = true;
			}

			Label lbl = (Label) control;
			lbl.setText(imgTag);
			// lbl.setSize(size);
//		} else if (control instanceof FileUpload) {
//			FileUpload lbl = (FileUpload) control;
//			lbl.setImage(CmsUiUtils.noImage(size));
//			lbl.setSize(new Point(size.getWidth(), size.getHeight()));
//			return loaded;
		} else
			loaded = false;

		return loaded;
	}

	public Cms2DSize getImageSize(M node) {
		// TODO optimise
		Image image = getSwtImage(node);
		return new Cms2DSize(image.getBounds().width, image.getBounds().height);
	}

}
