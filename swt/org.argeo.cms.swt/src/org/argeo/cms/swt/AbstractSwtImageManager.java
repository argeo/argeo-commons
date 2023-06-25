package org.argeo.cms.swt;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.ux.AbstractImageManager;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** Manages only public images so far. */
public abstract class AbstractSwtImageManager<M> extends AbstractImageManager<Control, M> {
	protected abstract ImageData getSwtImageData(M node);

	protected abstract String noImg(Cms2DSize size);

	@Override
	public Boolean load(M node, Control control, Cms2DSize preferredSize, URI link) {
		Cms2DSize imageSize = getImageSize(node);
		Cms2DSize size;
		String imgTag = null;
		if (preferredSize == null || imageSize.width() == 0 || imageSize.height() == 0
				|| (preferredSize.width() == 0 && preferredSize.height() == 0)) {
			if (imageSize.width() != 0 && imageSize.height() != 0) {
				// actual image size if completely known
				size = imageSize;
			} else {
				// no image if not completely known
				size = resizeTo(NO_IMAGE_SIZE, preferredSize != null ? preferredSize : imageSize);
				imgTag = noImg(size);
			}

		} else if (preferredSize.width() != 0 && preferredSize.height() != 0) {
			// given size if completely provided
			size = preferredSize;
		} else {
			// at this stage :
			// image is completely known
			assert imageSize.width() != 0 && imageSize.height() != 0;
			// one and only one of the dimension as been specified
			assert preferredSize.width() == 0 || preferredSize.height() == 0;
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
			StringBuilder sb = new StringBuilder();
			if (link != null)
				sb.append("<a href='").append(URLEncoder.encode(link.toString(), StandardCharsets.UTF_8)).append("'>");
			sb.append(imgTag);
			if (link != null)
				sb.append("</a>");

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
		ImageData imageData = getSwtImageData(node);
		return new Cms2DSize(imageData.width, imageData.height);
	}

}
