package org.argeo.cms.swt.acr;

import java.io.InputStream;

import org.argeo.api.acr.Content;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.cms.swt.AbstractSwtImageManager;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ux.CmsUxUtils;
import org.eclipse.swt.graphics.Image;

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
	protected Image getSwtImage(Content node) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String noImg(Cms2DSize size) {
		String dataPath = "";
		return CmsUxUtils.img(dataPath, size);
	}

	protected String getDataPathForUrl(Content content) {
		return CmsSwtUtils.cleanPathForUrl(getDataPath(content));
	}

	/** A path in the node repository */
	protected String getDataPath(Content node) {
		// TODO make it more configurable?
		StringBuilder buf = new StringBuilder(CmsConstants.PATH_API_ACR);
		buf.append(node.getPath());
		return buf.toString();
	}
}
