package org.argeo.cms.swt.acr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.argeo.api.acr.Content;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.acr.SvgAttrs;
import org.argeo.cms.swt.AbstractSwtImageManager;
import org.argeo.cms.ux.CmsUxUtils;
import org.eclipse.swt.graphics.ImageData;

/** Implementation of {@link AbstractSwtImageManager} based on ACR. */
public class AcrSwtImageManager extends AbstractSwtImageManager<Content> {

	@Override
	public String getImageUrl(Content node) {
		return getDataPathForUrl(node);
	}

	@Override
	public String uploadImage(Content context, Content uploadFolder, String fileName, InputStream in,
			String contentType) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ImageData getSwtImageData(Content node) {
		try (InputStream in = node.open(InputStream.class)) {
			ImageData imageData = new ImageData(in);
			return imageData;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String noImg(Cms2DSize size) {
		String dataPath = "";
		return CmsUxUtils.img(dataPath, size);
	}

	protected String getDataPathForUrl(Content content) {
		return ContentUtils.getDataPathForUrl(content);
	}

	@Override
	public Cms2DSize getImageSize(Content node) {
		// TODO cache it?
		Optional<Integer> width = node.get(SvgAttrs.width, Integer.class);
		Optional<Integer> height = node.get(SvgAttrs.height, Integer.class);
		if (!width.isEmpty() && !height.isEmpty())
			return new Cms2DSize(width.get(), height.get());
		return super.getImageSize(node);
	}

}
