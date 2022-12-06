package org.argeo.cms.ux;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.cms.ux.Cms2DSize;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.cms.util.CurrentSubject;

public class CmsUxUtils {
	public static ContentSession getContentSession(ContentRepository contentRepository, CmsView cmsView) {
		return CurrentSubject.callAs(cmsView.getCmsSession().getSubject(), () -> contentRepository.get());
	}

	public static String getTitle(Content content) {
		return content.getName().getLocalPart();
	}

	/** singleton */
	private CmsUxUtils() {

	}

	public static StringBuilder imgBuilder(String src, String width, String height) {
		return new StringBuilder(64).append("<img width='").append(width).append("' height='").append(height)
				.append("' src='").append(src).append("'");
	}

	public static String img(String src, String width, String height) {
		return imgBuilder(src, width, height).append("/>").toString();
	}

	public static String img(String src, Cms2DSize size) {
		return img(src, Integer.toString(size.getWidth()), Integer.toString(size.getHeight()));
	}
}
